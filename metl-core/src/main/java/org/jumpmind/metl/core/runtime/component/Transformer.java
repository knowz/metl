/**
 * Licensed to JumpMind Inc under one or more contributor
 * license agreements.  See the NOTICE file distributed
 * with this work for additional information regarding
 * copyright ownership.  JumpMind Inc licenses this file
 * to you under the GNU General Public License, version 3.0 (GPLv3)
 * (the "License"); you may not use this file except in compliance
 * with the License.
 *
 * You should have received a copy of the GNU General Public License,
 * version 3.0 (GPLv3) along with this library; if not, see
 * <http://www.gnu.org/licenses/>.
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.jumpmind.metl.core.runtime.component;

import static org.apache.commons.lang.StringUtils.isNotBlank;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jumpmind.metl.core.model.ComponentAttributeSetting;
import org.jumpmind.metl.core.model.Model;
import org.jumpmind.metl.core.model.ModelAttribute;
import org.jumpmind.metl.core.model.ModelEntity;
import org.jumpmind.metl.core.runtime.EntityData;
import org.jumpmind.metl.core.runtime.Message;
import org.jumpmind.metl.core.runtime.flow.ISendMessageCallback;

public class Transformer extends AbstractComponentRuntime {

    public static final String TYPE = "Transformer";
    
    public static String TRANSFORM_EXPRESSION = "transform.expression";

    Map<String, String> transformsByAttributeId = new HashMap<String, String>();
   
    
    @Override
    protected void start() {
        transformsByAttributeId.clear();
        
        List<ComponentAttributeSetting> settings = getComponent().getAttributeSettings();
        for (ComponentAttributeSetting setting : settings) {
            if (setting.getName().equals(TRANSFORM_EXPRESSION)) {
                if (isNotBlank(setting.getValue())) {
                    transformsByAttributeId.put(setting.getAttributeId(), setting.getValue());
                }
            }
        }
    }

    @Override
    public void handle( Message inputMessage, ISendMessageCallback callback, boolean unitOfWorkBoundaryReached) {
        Model inputModel = getComponent().getInputModel();
        List<EntityData> inDatas = inputMessage.getPayload();
        ArrayList<EntityData> outDatas = new ArrayList<EntityData>(inDatas.size());
        
        for (EntityData inData : inDatas) {
            EntityData outData = new EntityData();
            outDatas.add(outData);
            Set<String> attributeIds = new HashSet<String>(inData.keySet());
            attributeIds.addAll(transformsByAttributeId.keySet());
            for (String attributeId : attributeIds) {
                String transform = transformsByAttributeId.get(attributeId);
                Object value = inData.get(attributeId);
                if (isNotBlank(transform)) {
                    ModelAttribute attribute = inputModel.getAttributeById(attributeId);
                    ModelEntity entity = inputModel.getEntityById(attribute.getEntityId());                    
                    value = ModelAttributeScriptHelper.eval(attribute, value, entity, inData, transform);
                }
                if (value != ModelAttributeScriptHelper.REMOVE_ATTRIBUTE) {
                    outData.put(attributeId, value);
                }
            }            
            getComponentStatistics().incrementNumberEntitiesProcessed();
        }
        
        callback.sendMessage(outDatas, unitOfWorkBoundaryReached);
    }    

}