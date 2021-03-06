/**
 * Copyright (C) 2012 LinkedIn Inc <opensource@linkedin.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.linkedin.helix.manager;

import java.util.List;

import com.linkedin.helix.ConfigChangeListener;
import com.linkedin.helix.ControllerChangeListener;
import com.linkedin.helix.CurrentStateChangeListener;
import com.linkedin.helix.ExternalViewChangeListener;
import com.linkedin.helix.IdealStateChangeListener;
import com.linkedin.helix.LiveInstanceChangeListener;
import com.linkedin.helix.MessageListener;
import com.linkedin.helix.NotificationContext;
import com.linkedin.helix.model.CurrentState;
import com.linkedin.helix.model.ExternalView;
import com.linkedin.helix.model.IdealState;
import com.linkedin.helix.model.InstanceConfig;
import com.linkedin.helix.model.LiveInstance;
import com.linkedin.helix.model.Message;

public class MockListener implements IdealStateChangeListener, LiveInstanceChangeListener,
    ConfigChangeListener, CurrentStateChangeListener, ExternalViewChangeListener,
    ControllerChangeListener, MessageListener

{
  public boolean isIdealStateChangeListenerInvoked = false;
  public boolean isLiveInstanceChangeListenerInvoked = false;
  public boolean isCurrentStateChangeListenerInvoked = false;
  public boolean isMessageListenerInvoked = false;
  public boolean isConfigChangeListenerInvoked = false;
  public boolean isExternalViewChangeListenerInvoked = false;
  public boolean isControllerChangeListenerInvoked = false;

  public void reset()
  {
    isIdealStateChangeListenerInvoked = false;
    isLiveInstanceChangeListenerInvoked = false;
    isCurrentStateChangeListenerInvoked = false;
    isMessageListenerInvoked = false;
    isConfigChangeListenerInvoked = false;
    isExternalViewChangeListenerInvoked = false;
    isControllerChangeListenerInvoked = false;
  }

  @Override
  public void onIdealStateChange(List<IdealState> idealState, NotificationContext changeContext)
  {
    isIdealStateChangeListenerInvoked = true;
  }

  @Override
  public void onLiveInstanceChange(List<LiveInstance> liveInstances, NotificationContext changeContext)
  {
    isLiveInstanceChangeListenerInvoked = true;
  }

  @Override
  public void onConfigChange(List<InstanceConfig> configs, NotificationContext changeContext)
  {
    isConfigChangeListenerInvoked = true;
  }

  @Override
  public void onStateChange(String instanceName,
                            List<CurrentState> statesInfo,
                            NotificationContext changeContext)
  {
    isCurrentStateChangeListenerInvoked = true;
  }

  @Override
  public void onExternalViewChange(List<ExternalView> externalViewList,
                                   NotificationContext changeContext)
  {
    isExternalViewChangeListenerInvoked = true;
  }

  @Override
  public void onControllerChange(NotificationContext changeContext)
  {
    isControllerChangeListenerInvoked = true;
  }

  @Override
  public void onMessage(String instanceName,
                        List<Message> messages,
                        NotificationContext changeContext)
  {
    isMessageListenerInvoked = true;
  }
}
