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

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.FastDateFormat;
import org.jumpmind.metl.core.model.ModelAttribute;
import org.jumpmind.metl.core.model.ModelEntity;
import org.jumpmind.metl.core.runtime.EntityData;
import org.jumpmind.util.FormatUtils;
import org.springframework.core.LocalVariableTableParameterNameDiscoverer;

public class ModelAttributeScriptHelper {

    Object value;
    
    EntityData data;
    
    ModelAttribute attribute;
    
    ModelEntity entity;
    
    public static final RemoveAttribute REMOVE_ATTRIBUTE = new RemoveAttribute();
    
    static private ThreadLocal<ScriptEngine> scriptEngine = new ThreadLocal<ScriptEngine>();

    public ModelAttributeScriptHelper(ModelAttribute attribute, ModelEntity entity, EntityData data, Object value) {
        this.value = value;
        this.data = data;
        this.attribute = attribute;
        this.entity = entity;
    }
    
    public Object nullvalue() {
        return null;
    }
    
    public Integer integer() {
        String text = value != null ? value.toString() : "0";
        text = isNotBlank(text) ? text : "0";
        return Integer.parseInt(text);
    }
    
    public String abbreviate(int maxwidth) {
        String text = value != null ? value.toString() : "";
        return StringUtils.abbreviate(text, maxwidth);
    }

    public String left(int length) {
        return StringUtils.left(value != null ? value.toString() : "", length);
    }

    public String right(int length) {
        return StringUtils.right(value != null ? value.toString() : "", length);
    }

    public String rpad(String padChar, int length) {
        String text = value != null ? value.toString() : "";
        return StringUtils.rightPad(text, length, padChar);
    }

    public String lpad(String padChar, int length) {
        String text = value != null ? value.toString() : "";
        return StringUtils.leftPad(text, length, padChar);
    }

    public String substr(int start, int end) {
        String text = value != null ? value.toString() : "";
        return StringUtils.substring(text, start, end);
    }

    public String lower() {
        String text = value != null ? value.toString() : "";
        return StringUtils.lowerCase(text);
    }

    public String upper() {
        String text = value != null ? value.toString() : "";
        return StringUtils.upperCase(text);
    }

    public String trim() {
        String text = value != null ? value.toString() : "";
        return StringUtils.trim(text);
    }

    public String format(String spec) {
        return String.format(spec, value);
    }

    public String replace(String searchString, String replacement) {
        String text = value != null ? value.toString() : "";
        return StringUtils.replace(text, searchString, replacement);
    }
    
    public Date currentdate() {
        return new Date();
    }
    
    public String currentdate(String format) {
        Date currentDate = new Date();
        return formatdate(format, currentDate);
                
    }
    
    public RemoveAttribute remove() {
        return REMOVE_ATTRIBUTE;
    }
    
    public Date parsedate(String pattern, String nulldate) {
        String text = value != null ? value.toString() : "";
        if (isNotBlank(text) && !text.equals(nulldate)) {
            return parseDateFromText(pattern, text);
        } else {
            return null;
        }
    }
    
    public Date parsedate(String pattern) {
        String text = value != null ? value.toString() : "";
        return parseDateFromText(pattern, text);
    }
    
    public String formatdate(String pattern) {
        FastDateFormat formatter = FastDateFormat.getInstance(pattern);
        if (value instanceof Date) {
            return formatter.format((Date) value);
        } else if (value != null) {
            String text = value != null ? value.toString() : "";
            Date dateToParse = parseDateFromText(pattern, text);
            if (dateToParse != null) {
                return formatter.format((Date) value);
            } else {
                return "Not a datetime";
            }
        } else {
            return "";
        }
    }

    private String formatdate(String pattern, Date value) {
        FastDateFormat formatter = FastDateFormat.getInstance(pattern);
        if (value != null) {
            return formatter.format(value);
        } else {
            return "";
        }
    }
    
    public String parseAndFormatDate(String parsePattern, String formatPattern) {
        Date dateToFormat = parsedate(parsePattern);
        return formatdate(formatPattern, dateToFormat);
    }
    
    private Date parseDateFromText(String pattern, String valueToParse) {
        if (isNotBlank(valueToParse)) {
            return FormatUtils.parseDate(valueToParse, new String[] { pattern});
        } else {
            return null;
        }        
    }
    
    public String stringConstant(String value) {
        return value;
    }
    
    protected Object eval() {
        return value;
    }

    public static String[] getSignatures() {
        List<String> signatures = new ArrayList<String>();
        Method[] methods = ModelAttributeScriptHelper.class.getMethods();
        LocalVariableTableParameterNameDiscoverer discoverer = new LocalVariableTableParameterNameDiscoverer();
        for (Method method : methods) {
            if (method.getDeclaringClass().equals(ModelAttributeScriptHelper.class)
                    && Modifier.isPublic(method.getModifiers())
                    && !Modifier.isStatic(method.getModifiers())) {
                StringBuilder sig = new StringBuilder(method.getName());
                sig.append("(");
                String[] names = discoverer.getParameterNames(method);
                for (String name : names) {
                    sig.append(name);
                    sig.append(",");

                }
                if (names.length > 0) {
                    sig.replace(sig.length() - 1, sig.length(), ")");
                } else {
                    sig.append(")");
                }
                signatures.add(sig.toString());
            }
        }
        Collections.sort(signatures);
        return signatures.toArray(new String[signatures.size()]);
    }

    public static Object eval(ModelAttribute attribute, Object value, ModelEntity entity, EntityData data, String expression) {
        ScriptEngine engine = scriptEngine.get();
        if (engine == null) {
            ScriptEngineManager factory = new ScriptEngineManager();
            engine = factory.getEngineByName("groovy");
            scriptEngine.set(engine);
        }

        engine.put("value", value);
        engine.put("data", data);
        engine.put("entity", entity);
        engine.put("attribute", attribute);

        try {
            String importString = "import org.jumpmind.metl.core.runtime.component.ModelAttributeScriptHelper;\n";
            String code = String.format(
                    "return new ModelAttributeScriptHelper(attribute, entity, data, value) { public Object eval() { return %s } }.eval()",
                    expression);
            return engine.eval(importString + code);
        } catch (ScriptException e) {
            throw new RuntimeException("Unable to evaluate groovy script.  Attribute ==> " + attribute.getName() + 
                    ".  Value ==> " + value.toString() + "." + 
                    e.getCause().getMessage(), e);
        }
    }
    
    static class RemoveAttribute {
        
    }

}