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

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.jumpmind.metl.core.model.Resource;
import org.jumpmind.metl.core.runtime.MisconfiguredException;
import org.jumpmind.metl.core.runtime.component.helpers.ComponentBuilder;
import org.jumpmind.metl.core.runtime.component.helpers.MessageTestHelper;
import org.jumpmind.metl.core.runtime.resource.FileInfo;
import org.jumpmind.metl.core.runtime.resource.IDirectory;
import org.jumpmind.metl.core.runtime.resource.LocalFile;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
public class FilePollerTest extends AbstractComponentRuntimeTestSupport {

    @Test
    @Override
    public void testStartDefaults() {
        Resource resource = mock(Resource.class);
        when(resource.getType()).thenReturn(LocalFile.TYPE);
        setupStart(new ComponentBuilder().withResource(resource).build());

        ((FilePoller) spy).start();

        Assert.assertEquals(false, ((FilePoller) spy).useTriggerFile);
        Assert.assertEquals(true, ((FilePoller) spy).cancelOnNoFiles);
        Assert.assertEquals(FilePoller.ACTION_NONE, ((FilePoller) spy).actionOnSuccess);
        Assert.assertEquals(FilePoller.ACTION_NONE, ((FilePoller) spy).actionOnError);
        Assert.assertEquals(FilePoller.SORT_MODIFIED, ((FilePoller) spy).fileSortOption);
    }

    @Test
    public void testStartDefaultsInvalidResourceType() {
        Resource resource = mock(Resource.class);
        when(resource.getType()).thenReturn("test");
        setupStart(new ComponentBuilder().withResource(resource).build());

        try {
            ((FilePoller) spy).start();
        } catch (Exception e) {
            Assert.assertTrue(e instanceof MisconfiguredException);
        }
    }

    @Test
    @Override
    public void testStartWithValues() {
        Resource resource = mock(Resource.class);
        when(resource.getType()).thenReturn(LocalFile.TYPE);
        setupStart(new ComponentBuilder().withResource(resource).build());

        properties.put(FilePoller.SETTING_FILE_PATTERN, "*.jar");
        properties.put(FilePoller.SETTING_TRIGGER_FILE_PATH, "/trigger");
        properties.put(FilePoller.SETTING_USE_TRIGGER_FILE, "true");
        properties.put(FilePoller.SETTING_CANCEL_ON_NO_FILES, "true");
        properties.put(FilePoller.SETTING_ACTION_ON_SUCCESS, FilePoller.ACTION_DELETE);
        properties.put(FilePoller.SETTING_ACTION_ON_ERROR, FilePoller.ACTION_DELETE);
        properties.put(FilePoller.SETTING_ARCHIVE_ON_ERROR_PATH, "/archive-fail");
        properties.put(FilePoller.SETTING_ARCHIVE_ON_SUCCESS_PATH, "/archive-success");
        properties.put(FilePoller.SETTING_MAX_FILES_TO_POLL, "10");
        properties.put(FilePoller.SETTING_FILE_SORT_ORDER, FilePoller.SORT_NAME);

        ((FilePoller) spy).start();

        Assert.assertEquals("*.jar", ((FilePoller) spy).filePattern);
        Assert.assertEquals("/trigger", ((FilePoller) spy).triggerFilePath);
        Assert.assertEquals(true, ((FilePoller) spy).useTriggerFile);
        Assert.assertEquals(true, ((FilePoller) spy).cancelOnNoFiles);
        Assert.assertEquals(FilePoller.ACTION_DELETE, ((FilePoller) spy).actionOnSuccess);
        Assert.assertEquals(FilePoller.ACTION_DELETE, ((FilePoller) spy).actionOnError);
        Assert.assertEquals("/archive-fail", ((FilePoller) spy).archiveOnErrorPath);
        Assert.assertEquals("/archive-success", ((FilePoller) spy).archiveOnSuccessPath);
        Assert.assertEquals(10, ((FilePoller) spy).maxFilesToPoll);
        Assert.assertEquals(FilePoller.SORT_NAME, ((FilePoller) spy).fileSortOption);
    }

    @Test
    @Override
    public void testHandleStartupMessage() {
        setupHandle(true);
        ((FilePoller) spy).setRunWhen(Execute.PER_UNIT_OF_WORK);
        MessageTestHelper.addControlMessage(this, "test", false);
        MessageTestHelper.addOutputMonitor(this, "file.txt");
        runHandle();
        assertHandle(1);
    }

    @Test
    @Override
    public void testHandleUnitOfWorkLastMessage() {
        setupHandle(true);
        ((FilePoller) spy).setRunWhen(Execute.PER_UNIT_OF_WORK);
        MessageTestHelper.addControlMessage(this, "test", true);
        MessageTestHelper.addOutputMonitor(this, "file.txt");
        runHandle();
        assertHandle(1);
    }

    @Test
    @Override
    public void testHandleNormal() throws Exception {
        setupHandle(true);

        MessageTestHelper.addInputMessage(this, true, "step1", "");
        MessageTestHelper.addOutputMonitor(this, "file.txt");

        runHandle();
        assertHandle(1);
    }

    @Test
    public void testHandleUseTriggerFile() {
        setupHandle(true);
        ((FilePoller) spy).useTriggerFile = true;

        MessageTestHelper.addInputMessage(this, true, "step1", "");
        MessageTestHelper.addOutputMonitor(this, "file.txt");

        runHandle();
        assertHandle(1);
    }

    @Test
    public void testHandleCancelOnShutdownWithTriggerFile() {
        setupHandle(false);

        ((FilePoller) spy).cancelOnNoFiles = true;
        ((FilePoller) spy).useTriggerFile = true;

        IDirectory mockDirectory = mock(IDirectory.class);
        when(((FilePoller) spy).getResourceReference()).thenReturn(mockDirectory);
        when(mockDirectory.listFiles()).thenReturn(new ArrayList<>());

        MessageTestHelper.addInputMessage(this, true, "step1", "");
        MessageTestHelper.addOutputMonitor(this, 0, 1);

        runHandle();
        assertHandle(0);
    }

    @Override
    protected String getComponentId() {
        return FilePoller.TYPE;
    }

    public void setupHandle(boolean fileExists) {
        super.setupHandle();

        ((FilePoller) spy).maxFilesToPoll = 5;

        IDirectory mockDirectory = mock(IDirectory.class);
        doReturn(mockDirectory).when((AbstractComponentRuntime) spy).getResourceReference();

        List<FileInfo> fileInfos = new ArrayList<>();
        fileInfos.add(new FileInfo("file.txt", false, System.currentTimeMillis(), 100000));
        when(mockDirectory.listFiles(any())).thenReturn(fileInfos);

        ((FilePoller) spy).setRunWhen(Execute.PER_MESSAGE);
    }

}
