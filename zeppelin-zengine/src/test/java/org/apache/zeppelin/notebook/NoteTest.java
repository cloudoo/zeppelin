/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.zeppelin.notebook;

import com.google.common.base.Optional;

import org.apache.commons.lang.StringUtils;
import org.apache.zeppelin.interpreter.Interpreter;
import org.apache.zeppelin.interpreter.InterpreterSetting;
import org.apache.zeppelin.notebook.repo.NotebookRepo;
import org.apache.zeppelin.scheduler.Scheduler;
import org.apache.zeppelin.search.SearchService;
import org.apache.zeppelin.user.Credentials;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class NoteTest {
  @Mock
  NotebookRepo repo;

  @Mock
  NoteInterpreterLoader replLoader;

  @Mock
  JobListenerFactory jobListenerFactory;

  @Mock
  SearchService index;

  @Mock
  Credentials credentials;

  @Mock
  Interpreter interpreter;

  @Mock
  Scheduler scheduler;

  @Test
  public void runNormalTest() {
    when(replLoader.get("spark")).thenReturn(interpreter);
    when(interpreter.getScheduler()).thenReturn(scheduler);

    String pText = "%spark sc.version";
    Note note = new Note(repo, replLoader, jobListenerFactory, index, credentials);
    Paragraph p = note.addParagraph();
    p.setText(pText);
    note.run(p.getId());

    ArgumentCaptor<Paragraph> pCaptor = ArgumentCaptor.forClass(Paragraph.class);
    verify(scheduler, only()).submit(pCaptor.capture());
    verify(replLoader, only()).get("spark");

    assertEquals("Paragraph text", pText, pCaptor.getValue().getText());
  }

  @Test
  public void runJdbcTest() {
    when(replLoader.get("mysql")).thenReturn(null);
    when(replLoader.get("jdbc")).thenReturn(interpreter);
    when(interpreter.getScheduler()).thenReturn(scheduler);

    String pText = "%mysql show databases";
    Note note = new Note(repo, replLoader, jobListenerFactory, index, credentials);
    Paragraph p = note.addParagraph();
    p.setText(pText);
    note.run(p.getId());

    ArgumentCaptor<Paragraph> pCaptor = ArgumentCaptor.forClass(Paragraph.class);
    verify(scheduler, only()).submit(pCaptor.capture());
    verify(replLoader, times(2)).get(anyString());

    assertEquals("Change paragraph text", "%jdbc(mysql) show databases", pCaptor.getValue().getEffectiveText());
    assertEquals("Change paragraph text", pText, pCaptor.getValue().getText());
  }

  @Test
  public void putDefaultReplNameIfInterpreterSettingAbsent() {
    when(replLoader.getDefaultInterpreterSetting())
            .thenReturn(Optional.<InterpreterSetting>absent());

    Note note = new Note(repo, replLoader, jobListenerFactory, index, credentials);
    note.putDefaultReplName();

    assertEquals(StringUtils.EMPTY, note.getLastReplName());
    assertEquals(StringUtils.EMPTY, note.getLastInterpreterName());
  }

  @Test
  public void putDefaultReplNameIfInterpreterSettingPresent() {
    InterpreterSetting interpreterSetting = Mockito.mock(InterpreterSetting.class);
    when(interpreterSetting.getGroup()).thenReturn("spark");
    when(replLoader.getDefaultInterpreterSetting())
            .thenReturn(Optional.of(interpreterSetting));

    Note note = new Note(repo, replLoader, jobListenerFactory, index, credentials);
    note.putDefaultReplName();

    assertEquals("spark", note.getLastReplName());
    assertEquals("%spark", note.getLastInterpreterName());
  }

  @Test
  public void addParagraphWithLastReplName() {
    InterpreterSetting interpreterSetting = Mockito.mock(InterpreterSetting.class);
    when(interpreterSetting.getGroup()).thenReturn("spark");
    when(replLoader.getDefaultInterpreterSetting())
            .thenReturn(Optional.of(interpreterSetting));

    Note note = new Note(repo, replLoader, jobListenerFactory, index, credentials);
    note.putDefaultReplName(); //set lastReplName

    Paragraph p = note.addParagraph();

    assertEquals("%spark ", p.getText());
  }

  @Test
  public void insertParagraphWithLastReplName() {
    InterpreterSetting interpreterSetting = Mockito.mock(InterpreterSetting.class);
    when(interpreterSetting.getGroup()).thenReturn("spark");
    when(replLoader.getDefaultInterpreterSetting())
            .thenReturn(Optional.of(interpreterSetting));

    Note note = new Note(repo, replLoader, jobListenerFactory, index, credentials);
    note.putDefaultReplName(); //set lastReplName

    Paragraph p = note.insertParagraph(note.getParagraphs().size());

    assertEquals("%spark ", p.getText());
  }

  @Test
  public void setLastReplName() {
    String paragraphId = "HelloWorld";
    Note note = Mockito.spy(new Note(repo, replLoader, jobListenerFactory, index, credentials));
    Paragraph mockParagraph = Mockito.mock(Paragraph.class);
    when(note.getParagraph(paragraphId)).thenReturn(mockParagraph);
    when(mockParagraph.getRequiredReplName()).thenReturn("spark");

    note.setLastReplName(paragraphId);

    assertEquals("spark", note.getLastReplName());
  }
}
