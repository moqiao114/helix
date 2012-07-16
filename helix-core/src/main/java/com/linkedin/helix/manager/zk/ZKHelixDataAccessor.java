package com.linkedin.helix.manager.zk;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.I0Itec.zkclient.DataUpdater;
import org.I0Itec.zkclient.exception.ZkNoNodeException;
import org.apache.log4j.Logger;

import com.linkedin.helix.BaseDataAccessor;
import com.linkedin.helix.GroupCommit;
import com.linkedin.helix.HelixDataAccessor;
import com.linkedin.helix.HelixException;
import com.linkedin.helix.HelixProperty;
import com.linkedin.helix.PropertyKey;
import com.linkedin.helix.PropertyKey.Builder;
import com.linkedin.helix.PropertyType;
import com.linkedin.helix.ZNRecord;
import com.linkedin.helix.ZNRecordUpdater;
import com.linkedin.helix.controller.restlet.ZNRecordUpdate;
import com.linkedin.helix.controller.restlet.ZNRecordUpdate.OpCode;
import com.linkedin.helix.controller.restlet.ZkPropertyTransferClient;
import com.linkedin.helix.model.LiveInstance;

public class ZKHelixDataAccessor implements HelixDataAccessor
{
  private static Logger                    LOG          =
                                                            Logger.getLogger(ZKHelixDataAccessor.class);
  private final BaseDataAccessor<ZNRecord> _baseDataAccessor;
  private final String _clusterName;
  private final Builder _propertyKeyBuilder;
  final ZkPropertyTransferClient _zkPropertyTransferClient;
  private final GroupCommit _groupCommit = new GroupCommit();

  public ZKHelixDataAccessor(String clusterName,
                             BaseDataAccessor<ZNRecord> baseDataAccessor)
  {
    _clusterName = clusterName;
    _baseDataAccessor = baseDataAccessor;
    _propertyKeyBuilder = new PropertyKey.Builder(_clusterName);
    _zkPropertyTransferClient
      = new ZkPropertyTransferClient(ZkPropertyTransferClient.DEFAULT_MAX_CONCURRENTTASKS);
  }

  @Override
  public <T extends HelixProperty> boolean createProperty(PropertyKey key, T value)
  {
    PropertyType type = key.getType();
    String path = key.getPath();
    int options = constructOptions(type);
    return _baseDataAccessor.create(path, value.getRecord(), options);
  }

  @Override
  public <T extends HelixProperty> boolean setProperty(PropertyKey key, T value)
  {
    PropertyType type = key.getType();
    if (!value.isValid())
    {
      throw new HelixException("The ZNRecord for " + type + " is not valid.");
    }

    String path = key.getPath();
    int options = constructOptions(type);
    
    if(type.usePropertyTransferServer() && getPropertyTransferUrl() != null)
    {
      ZNRecordUpdate update = new ZNRecordUpdate(path, OpCode.SET, value.getRecord());
      _zkPropertyTransferClient.sendZNRecordUpdate(update, getPropertyTransferUrl());
      return true;
    }
    
    return _baseDataAccessor.set(path, value.getRecord(), options);
  }

  @Override
  public <T extends HelixProperty> boolean updateProperty(PropertyKey key,
      T value)
  {
    PropertyType type = key.getType();
    String path = key.getPath();
    int options = constructOptions(type);
    
    boolean success = false;
    switch (type)
    {
    case CURRENTSTATES:
      success = _groupCommit.commit(_baseDataAccessor, path, value.getRecord());
      break;
    default:
      if(type.usePropertyTransferServer())
      {
        if(getPropertyTransferUrl() != null)
        {
          ZNRecordUpdate update = new ZNRecordUpdate(path, OpCode.UPDATE, value.getRecord());
          _zkPropertyTransferClient.sendZNRecordUpdate(update, getPropertyTransferUrl());
          return true;
        }
        else
        {
          LOG.debug("getPropertyTransferUrl is null, direct set the value");
          //return true;
        }
      }
      success = _baseDataAccessor.update(path, new ZNRecordUpdater(value.getRecord()), options);
      break;
    }
    return success;
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T extends HelixProperty> T getProperty(PropertyKey key)
  {
    PropertyType type = key.getType();
    String path = key.getPath();
    int options = constructOptions(type);
    ZNRecord record = null;
    try
    {
      record = _baseDataAccessor.get(path, null, options);
    }
    catch (ZkNoNodeException e)
    {
      // OK
    }
    return (T) HelixProperty.convertToTypedInstance(key.getTypeClass(), record);
  }

  @Override
  public boolean removeProperty(PropertyKey key)
  {
    PropertyType type = key.getType();
    String path = key.getPath();
    return _baseDataAccessor.remove(path);
  }

  @Override
  public List<String> getChildNames(PropertyKey key)
  {
    PropertyType type = key.getType();
    String parentPath = key.getPath();
    int options = constructOptions(type);
    return _baseDataAccessor.getChildNames(parentPath, options);
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T extends HelixProperty> List<T> getChildValues(PropertyKey key)
  {
    PropertyType type = key.getType();
    String parentPath = key.getPath();
    int options = constructOptions(type);
    List<ZNRecord> children = _baseDataAccessor.getChildren(parentPath, null, options);
    List<HelixProperty> childValues = new ArrayList<HelixProperty>();
    for (ZNRecord record : children)
    {
      HelixProperty typedInstance =
          HelixProperty.convertToTypedInstance(key.getTypeClass(), record);
      childValues.add(typedInstance);
    }
    return (List<T>) childValues;
  }

  @Override
  public <T extends HelixProperty> Map<String, T> getChildValuesMap(PropertyKey key)
  {
    PropertyType type = key.getType();
    String parentPath = key.getPath();
    int options = constructOptions(type);
    List<ZNRecord> children = _baseDataAccessor.getChildren(parentPath, null, options);
    Map<String, T> childValuesMap = new HashMap<String, T>();
    for (ZNRecord record : children)
    {
      @SuppressWarnings("unchecked")
      T t = (T) HelixProperty.convertToTypedInstance(key.getTypeClass(), record);
      childValuesMap.put(record.getId(), t);
    }
    return childValuesMap;
  }

  @Override
  public Builder keyBuilder()
  {
    return _propertyKeyBuilder;
  }

  private int constructOptions(PropertyType type)
  {
    int options = 0;
    if (type.isPersistent())
    {
      options = options | BaseDataAccessor.Option.PERSISTENT;
    }
    else
    {
      options = options | BaseDataAccessor.Option.EPHEMERAL;
    }
    return options;
  }

  @Override
  public <T extends HelixProperty> boolean[] createChildren(List<PropertyKey> keys,
                                                            List<T> children)
  {
    // TODO: add validation
    int options = -1;
    List<String> paths = new ArrayList<String>();
    List<ZNRecord> records = new ArrayList<ZNRecord>();
    for (int i = 0; i < keys.size(); i++)
    {
      PropertyKey key = keys.get(i);
      PropertyType type = key.getType();
      String path = key.getPath();
      paths.add(path);
      HelixProperty value = children.get(i);
      records.add(value.getRecord());
      options = constructOptions(type);
    }
    return _baseDataAccessor.createChildren(paths, records, options);
  }

  @Override
  public <T extends HelixProperty> boolean[] setChildren(List<PropertyKey> keys,
                                                         List<T> children)
  {
    int options = -1;
    List<String> paths = new ArrayList<String>();
    List<ZNRecord> records = new ArrayList<ZNRecord>();
    for (int i = 0; i < keys.size(); i++)
    {
      PropertyKey key = keys.get(i);
      PropertyType type = key.getType();
      String path = key.getPath();
      paths.add(path);
      HelixProperty value = children.get(i);
      records.add(value.getRecord());
      options = constructOptions(type);
    }
    return _baseDataAccessor.setChildren(paths, records, options);

  }

  @Override
  public BaseDataAccessor<ZNRecord> getBaseDataAccessor()
  {
    return _baseDataAccessor;
  }

  @Override
  public <T extends HelixProperty> boolean[] updateChildren(List<String> paths,
                                                            List<DataUpdater<ZNRecord>> updaters,
                                                            int options)
  {
    return _baseDataAccessor.updateChildren(paths, updaters, options);
  }
  
  private String getPropertyTransferUrl()
  {
    LiveInstance leader = getProperty(keyBuilder().controllerLeader());
    if(leader != null)
    {
      return leader.getWebserviceUrl();
    }
    return null;
  }

  public void shutdown()
  {
    _zkPropertyTransferClient.shutdown();
  }
}
