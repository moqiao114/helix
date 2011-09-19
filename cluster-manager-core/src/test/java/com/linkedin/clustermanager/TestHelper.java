package com.linkedin.clustermanager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.I0Itec.zkclient.IDefaultNameSpace;
import org.I0Itec.zkclient.ZkServer;
import org.apache.log4j.Logger;

import com.linkedin.clustermanager.agent.zk.ZkClient;
import com.linkedin.clustermanager.controller.ClusterManagerMain;
import com.linkedin.clustermanager.controller.ClusterManagerMainException;
import com.linkedin.clustermanager.mock.storage.DummyProcess.DummyStateModel;
import com.linkedin.clustermanager.mock.storage.DummyProcess.DummyStateModelFactory;
import com.linkedin.clustermanager.participant.StateMachineEngine;
import com.linkedin.clustermanager.util.ZKClientPool;

public class TestHelper
{
  private static final Logger logger = Logger.getLogger(TestHelper.class);
  
  static public ZkServer startZkSever(final String zkAddress, final String rootNamespace) 
  {
    List<String> rootNamespaces = new ArrayList<String>();
    rootNamespaces.add(rootNamespace);
    return TestHelper.startZkSever(zkAddress, rootNamespaces);
  }
  
  static public ZkServer startZkSever(final String zkAddress, final List<String> rootNamespaces) 
  {
    final String logDir = "/tmp/logs";
    final String dataDir = "/tmp/dataDir";
    
    /*
    try
    {
      FileUtils.deleteDirectory(new File(dataDir));
      FileUtils.deleteDirectory(new File(logDir));
    }
    catch (IOException e)
    {
      logger.warn("fail to delete dir: " + dataDir + ", " + logDir + 
                  "\nexception:" + e);
    }
    */
    
    ZKClientPool.reset();
    
    IDefaultNameSpace defaultNameSpace = new IDefaultNameSpace()
    {
      @Override
      public void createDefaultNameSpace(org.I0Itec.zkclient.ZkClient zkClient)
      {
        for (String rootNamespace : rootNamespaces)
        {
          try
          {
            zkClient.deleteRecursive(rootNamespace);
          }
          catch (Exception e)
          {
            logger.error("fail to deleteRecursive path:" + rootNamespace + 
                         "\nexception:" + e);
          }
        }
      }
    };
   
    int port = Integer.parseInt(zkAddress.substring(zkAddress.lastIndexOf(':') + 1));
    ZkServer zkServer = new ZkServer(dataDir, logDir, defaultNameSpace, port);
    zkServer.start();
    
    return zkServer;
  }
  
  static public void stopZkServer(ZkServer zkServer)
  {
    if (zkServer != null)
    {
      zkServer.shutdown();
    }
  }
  
  /*
  public static Thread startDistClusterController(final String clusterName,
      final String instanceName, final String zkAddr)
  {
    Thread thread = new Thread(new Runnable()
    {
    
      @Override
      public void run()
      {
        // participate CONTROLLOR_CLUSTER and do leader election
        ClusterManager manager = null;
        try
        {
          manager = ClusterManagerFactory
              .getZKBasedManagerForControllerParticipant(clusterName, instanceName, zkAddr);
          
          DistClusterControllerStateModelFactory stateModelFactory 
             = new DistClusterControllerStateModelFactory(zkAddr);
          StateMachineEngine genericStateMachineHandler 
            = new StateMachineEngine(stateModelFactory);
          manager.addMessageListener(genericStateMachineHandler, instanceName);
          
          // DistClusterControllerElection leaderElection = new DistClusterControllerElection();
          // manager.addControllerListener(leaderElection);
          
          Thread.currentThread().join();
        }
        catch (InterruptedException e)
        {
          if (manager != null)
          {
            manager.disconnect();
          }
        }
        catch (Exception e)
        {
          // TODO Auto-generated catch block
            e.printStackTrace();
        }
      
      }
    
    });
  
    thread.start();
    return thread;
  }
  */
  
  /*
  public static Thread startDummyProcess(final String zkAddr, final String clusterName, 
                                         final String instanceName)
  {
    Thread thread = new Thread(new Runnable()
    {
      @Override
      public void run()
      {
        try
        {
          DummyProcess process = new DummyProcess(zkAddr, clusterName, instanceName, 
                                                  null, 0, null);
          process.start();
          Thread.currentThread().join();
        } 
        catch (Exception e)
        {
          e.printStackTrace();
        }
      }
    });
    
    thread.start();
    return thread;
  }
  */
  
  /**
   * start dummy cluster participant with a pre-created zkClient
   *   for testing session expiry
   * @param zkAddr
   * @param clusterName
   * @param instanceName
   * @param zkClient
   * @return
   */
  public static Thread startDummyProcess(final String zkAddr, final String clusterName, 
         final String instanceName, final ZkClient zkClient)
  {
    Thread thread = new Thread(new Runnable()
    {
      @Override
      public void run()
      {
        try
        {
          ClusterManager manager = ClusterManagerFactory
            .getZKBasedManagerForParticipant(clusterName, instanceName, zkAddr, zkClient);
          DummyStateModelFactory stateModelFactory = new DummyStateModelFactory(0);
          StateMachineEngine<DummyStateModel> genericStateMachineHandler 
            = new StateMachineEngine<DummyStateModel>(stateModelFactory);
          
          manager.addMessageListener(genericStateMachineHandler, instanceName);
          Thread.currentThread().join();
        }
        catch (InterruptedException e)
        {
          logger.info("thread:" + Thread.currentThread().getName() + " interrupted");
        }
        catch (Exception e)
        {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }
      }
    });
    
    thread.start();
    return thread;
  }

  /**
   * start cluster controller with a pre-created zkClient
   *   for testing session expiry
   * @param clusterName
   * @param controllerName
   * @param zkConnectString
   * @param zkClient
   * @return
   */
  public static Thread startClusterController(final String clusterName, 
    final String controllerName, final String zkConnectString, final String controllerMode, 
    final ZkClient zkClient)
  {
    Thread thread = new Thread(new Runnable()
    {
      @Override
      public void run()
      {
        ClusterManager manager = null;
        try
        {
          // ClusterManager manager = ClusterManagerFactory
          //  .getZKBasedManagerForController(clusterName, controllerName, zkConnectString,
          //      zkClient);
           manager = ClusterManagerMain.startClusterManagerMain(zkConnectString, 
                 clusterName, controllerName, controllerMode, zkClient);
          Thread.currentThread().join();
        } 
        catch (InterruptedException e)
        {
          if (manager != null)
          {
            manager.disconnect();
          }
          logger.info("thread:" + Thread.currentThread().getName() + " interrupted");
        }
        catch (Exception e)
        {
          e.printStackTrace();
        }
      }
    });
    
    thread.start();
    return thread;
  }

  
  public static Thread startClusterController(final String args)
  {
    Thread thread = new Thread(new Runnable()
    {
      @Override
      public void run()
      {
        try
        {
          ClusterManagerMain.main(createArgs(args));
        }
        catch (ClusterManagerMainException e)
        {
          ClusterManager manager = e.getManager();
          if (manager != null)
          {
            manager.disconnect();
          }
          logger.info("thread:" + Thread.currentThread().getName() + " interrupted");
        }
        catch (Exception e)
        {
          e.printStackTrace();
        }
      }
    });
    
    thread.start();
    return thread;
  }
  
  private static String[] createArgs(String str)
  {
    String[] split = str.split("[ ]+");
    logger.info("args=" + Arrays.toString(split));
    return split;
  }
  
}