/*
 * ApplicationInsights-Java
 * Copyright (c) Microsoft Corporation
 * All rights reserved.
 *
 * MIT License
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the ""Software""), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit
 * persons to whom the Software is furnished to do so, subject to the following conditions:
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
 * PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE
 * FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
 * OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 */

package com.microsoft.applicationinsights.agent.internal.processors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.microsoft.applicationinsights.agent.internal.wasbootstrap.configuration.Configuration.ExtractAttribute;
import com.microsoft.applicationinsights.agent.internal.wasbootstrap.configuration.Configuration.MatchType;
import com.microsoft.applicationinsights.agent.internal.wasbootstrap.configuration.Configuration.ProcessorAction;
import com.microsoft.applicationinsights.agent.internal.wasbootstrap.configuration.Configuration.ProcessorActionType;
import com.microsoft.applicationinsights.agent.internal.wasbootstrap.configuration.Configuration.ProcessorAttribute;
import com.microsoft.applicationinsights.agent.internal.wasbootstrap.configuration.Configuration.ProcessorConfig;
import com.microsoft.applicationinsights.agent.internal.wasbootstrap.configuration.Configuration.ProcessorIncludeExclude;
import com.microsoft.applicationinsights.agent.internal.wasbootstrap.configuration.Configuration.ProcessorType;
import com.microsoft.applicationinsights.agent.internal.wasbootstrap.configuration.ProcessorActionAdaptor;
import com.microsoft.applicationinsights.agent.internal.wascore.common.FriendlyException;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.trace.ReadableSpan;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

class ExporterWithAttributeProcessorTest {

  private final Tracer tracer = OpenTelemetrySdk.builder().build().getTracer("test");

  @Test
  void noActionTest() {
    MockExporter mockExporter = new MockExporter();
    ProcessorConfig config = new ProcessorConfig();
    config.type = ProcessorType.ATTRIBUTE;
    config.id = "noAction";

    assertThatThrownBy(() -> new ExporterWithAttributeProcessor(config, mockExporter))
        .isInstanceOf(FriendlyException.class);
  }

  @Test
  void inValidConfigTestWithNoValueInActionTest() {
    MockExporter mockExporter = new MockExporter();
    ProcessorConfig config = new ProcessorConfig();
    config.type = ProcessorType.ATTRIBUTE;
    config.id = "inValidConfigTestWithNoValueInAction";
    config.include = new ProcessorIncludeExclude();
    config.include.matchType = MatchType.STRICT;
    config.include.spanNames = Arrays.asList("svcA", "svcB");
    ProcessorAction action = new ProcessorAction();
    action.key = "testKey";
    action.action = ProcessorActionType.UPDATE;
    List<ProcessorAction> actions = new ArrayList<>();
    actions.add(action);
    config.actions = actions;

    assertThatThrownBy(() -> new ExporterWithAttributeProcessor(config, mockExporter))
        .isInstanceOf(FriendlyException.class);
  }

  @Test
  void inValidConfigTestWithInvalidIncludeTest() {
    MockExporter mockExporter = new MockExporter();
    ProcessorConfig config = new ProcessorConfig();
    config.type = ProcessorType.ATTRIBUTE;
    config.id = "inValidConfigTestWithInvalidInclude";
    config.include = new ProcessorIncludeExclude();
    config.include.matchType = MatchType.STRICT;
    config.include.spanNames = Arrays.asList("svcA", "svcB");
    ProcessorAction action = new ProcessorAction();
    action.key = "testKey";
    action.action = ProcessorActionType.UPDATE;
    List<ProcessorAction> actions = new ArrayList<>();
    actions.add(action);
    config.actions = actions;

    assertThatThrownBy(() -> new ExporterWithAttributeProcessor(config, mockExporter))
        .isInstanceOf(FriendlyException.class);
  }

  @Test
  void actionDeleteTest() {
    MockExporter mockExporter = new MockExporter();
    ProcessorConfig config = new ProcessorConfig();
    config.type = ProcessorType.ATTRIBUTE;
    config.id = "actionDelete";
    ProcessorAction action = new ProcessorAction();
    action.key = "testKey";
    action.action = ProcessorActionType.DELETE;
    List<ProcessorAction> actions = new ArrayList<>();
    actions.add(action);
    config.actions = actions;
    SpanExporter exampleExporter = new ExporterWithAttributeProcessor(config, mockExporter);

    Span span =
        tracer
            .spanBuilder("my span")
            .setAttribute("one", "1")
            .setAttribute("two", 2L)
            .setAttribute("testKey", "testValue")
            .setAttribute("TESTKEY", "testValue2")
            .startSpan();

    SpanData spanData = ((ReadableSpan) span).toSpanData();

    List<SpanData> spans = new ArrayList<>();
    spans.add(spanData);
    exampleExporter.export(spans);

    // verify that resulting spans are filtered in the way we want
    List<SpanData> result = mockExporter.getSpans();
    SpanData resultSpan = result.get(0);

    assertThat(resultSpan.getAttributes().get(AttributeKey.stringKey("testKey"))).isNull();
    assertThat(resultSpan.getAttributes().get(AttributeKey.stringKey("one"))).isNotNull();
    assertThat(resultSpan.getAttributes().get(AttributeKey.stringKey("TESTKEY"))).isNotNull();
  }

  @Test
  void actionInsertTest() {
    MockExporter mockExporter = new MockExporter();
    ProcessorConfig config = new ProcessorConfig();
    config.type = ProcessorType.ATTRIBUTE;
    config.id = "actionInsert";
    ProcessorAction action = new ProcessorAction();
    action.key = "testNewKey";
    action.value = "testNewValue";
    action.action = ProcessorActionType.INSERT;
    List<ProcessorAction> actions = new ArrayList<>();
    actions.add(action);
    config.actions = actions;
    SpanExporter exampleExporter = new ExporterWithAttributeProcessor(config, mockExporter);

    Span span =
        tracer
            .spanBuilder("my span")
            .setAttribute("one", "1")
            .setAttribute("two", 2L)
            .setAttribute("testKey", "testValue")
            .setAttribute("TESTKEY", "testValue2")
            .startSpan();

    SpanData spanData = ((ReadableSpan) span).toSpanData();

    List<SpanData> spans = new ArrayList<>();
    spans.add(spanData);
    exampleExporter.export(spans);

    // verify that resulting spans are filtered in the way we want
    List<SpanData> result = mockExporter.getSpans();
    SpanData resultSpan = result.get(0);

    assertThat(resultSpan.getAttributes().get(AttributeKey.stringKey("testNewKey"))).isNotNull();
    assertThat(resultSpan.getAttributes().get(AttributeKey.stringKey("testNewKey")))
        .isEqualTo("testNewValue");
  }

  @Test
  void actionInsertAndUpdateTest() {
    MockExporter mockExporter = new MockExporter();
    ProcessorConfig config = new ProcessorConfig();
    config.type = ProcessorType.ATTRIBUTE;
    config.id = "actionInsertAndUpdate";
    ProcessorAction action = new ProcessorAction();
    action.key = "testNewKey";
    action.value = "testNewValue";
    action.action = ProcessorActionType.INSERT;
    ProcessorAction updateAction = new ProcessorAction();
    updateAction.key = "testKey";
    updateAction.value = "testNewValue2";
    updateAction.action = ProcessorActionType.UPDATE;
    List<ProcessorAction> actions = new ArrayList<>();
    actions.add(action);
    actions.add(updateAction);
    config.actions = actions;
    SpanExporter exampleExporter = new ExporterWithAttributeProcessor(config, mockExporter);

    Span span =
        tracer
            .spanBuilder("my span")
            .setAttribute("one", "1")
            .setAttribute("two", 2L)
            .setAttribute("testKey", "testValue")
            .setAttribute("TESTKEY", "testValue2")
            .startSpan();

    SpanData spanData = ((ReadableSpan) span).toSpanData();

    List<SpanData> spans = new ArrayList<>();
    spans.add(spanData);
    exampleExporter.export(spans);

    // verify that resulting spans are filtered in the way we want
    List<SpanData> result = mockExporter.getSpans();
    SpanData resultSpan = result.get(0);

    assertThat(resultSpan.getAttributes().get(AttributeKey.stringKey("testNewKey"))).isNotNull();
    assertThat(resultSpan.getAttributes().get(AttributeKey.stringKey("testNewKey")))
        .isEqualTo("testNewValue");
    assertThat(resultSpan.getAttributes().get(AttributeKey.stringKey("testKey")))
        .isEqualTo("testNewValue2");
  }

  @Test
  void actionInsertAndUpdateSameAttributeTest() {
    MockExporter mockExporter = new MockExporter();
    ProcessorConfig config = new ProcessorConfig();
    config.type = ProcessorType.ATTRIBUTE;
    config.id = "actionInsertAndUpdate";
    ProcessorAction action = new ProcessorAction();
    action.key = "testNewKey";
    action.value = "testNewValue";
    action.action = ProcessorActionType.INSERT;
    ProcessorAction updateAction = new ProcessorAction();
    updateAction.key = "testNewKey";
    updateAction.value = "testNewValue2";
    updateAction.action = ProcessorActionType.UPDATE;
    List<ProcessorAction> actions = new ArrayList<>();
    actions.add(action);
    actions.add(updateAction);
    config.actions = actions;
    SpanExporter exampleExporter = new ExporterWithAttributeProcessor(config, mockExporter);

    Span span =
        tracer
            .spanBuilder("my span")
            .setAttribute("one", "1")
            .setAttribute("two", 2L)
            .setAttribute("testKey", "testValue")
            .setAttribute("TESTKEY", "testValue2")
            .startSpan();

    SpanData spanData = ((ReadableSpan) span).toSpanData();

    List<SpanData> spans = new ArrayList<>();
    spans.add(spanData);
    exampleExporter.export(spans);

    // verify that resulting spans are filtered in the way we want
    List<SpanData> result = mockExporter.getSpans();
    SpanData resultSpan = result.get(0);

    assertThat(resultSpan.getAttributes().get(AttributeKey.stringKey("testNewKey"))).isNotNull();
    assertThat(resultSpan.getAttributes().get(AttributeKey.stringKey("testNewKey")))
        .isEqualTo("testNewValue2");
  }

  @Test
  void actionInsertWithDuplicateTest() {
    MockExporter mockExporter = new MockExporter();
    ProcessorConfig config = new ProcessorConfig();
    config.type = ProcessorType.ATTRIBUTE;
    config.id = "actionInsertWithDuplicate";
    ProcessorAction action = new ProcessorAction();
    action.key = "testKey";
    action.value = "testNewValue";
    action.action = ProcessorActionType.INSERT;
    List<ProcessorAction> actions = new ArrayList<>();
    actions.add(action);
    config.actions = actions;
    SpanExporter exampleExporter = new ExporterWithAttributeProcessor(config, mockExporter);

    Span span =
        tracer
            .spanBuilder("my span")
            .setAttribute("one", "1")
            .setAttribute("two", 2L)
            .setAttribute("testKey", "testValue")
            .setAttribute("TESTKEY", "testValue2")
            .startSpan();

    SpanData spanData = ((ReadableSpan) span).toSpanData();

    List<SpanData> spans = new ArrayList<>();
    spans.add(spanData);
    exampleExporter.export(spans);

    // verify that resulting spans are filtered in the way we want
    List<SpanData> result = mockExporter.getSpans();
    SpanData resultSpan = result.get(0);

    assertThat(resultSpan.getAttributes().get(AttributeKey.stringKey("testKey")))
        .isEqualTo("testValue");
    assertThat(resultSpan.getAttributes().get(AttributeKey.stringKey("testKey")))
        .isNotEqualTo("testNewValue");
    assertThat(resultSpan.getAttributes().get(AttributeKey.stringKey("one"))).isEqualTo("1");
  }

  @Test
  void actionInsertFromAttributeTest() {
    MockExporter mockExporter = new MockExporter();
    ProcessorConfig config = new ProcessorConfig();
    config.type = ProcessorType.ATTRIBUTE;
    config.id = "actionInsertFromAttribute";
    ProcessorAction action = new ProcessorAction();
    action.key = "testKey3";
    action.fromAttribute = "testKey2";
    action.action = ProcessorActionType.INSERT;
    List<ProcessorAction> actions = new ArrayList<>();
    actions.add(action);
    config.actions = actions;
    SpanExporter exampleExporter = new ExporterWithAttributeProcessor(config, mockExporter);

    Span span =
        tracer
            .spanBuilder("my span")
            .setAttribute("one", "1")
            .setAttribute("two", 2L)
            .setAttribute("testKey", "testValue")
            .setAttribute("TESTKEY", "testValue2")
            .setAttribute("testKey2", "testValue2")
            .startSpan();

    SpanData spanData = ((ReadableSpan) span).toSpanData();

    List<SpanData> spans = new ArrayList<>();
    spans.add(spanData);
    exampleExporter.export(spans);

    // verify that resulting spans are filtered in the way we want
    List<SpanData> result = mockExporter.getSpans();
    SpanData resultSpan = result.get(0);

    assertThat(resultSpan.getAttributes().get(AttributeKey.stringKey("testKey")))
        .isEqualTo("testValue");
    assertThat(resultSpan.getAttributes().get(AttributeKey.stringKey("testKey3")))
        .isEqualTo("testValue2");
    assertThat(resultSpan.getAttributes().get(AttributeKey.stringKey("one"))).isEqualTo("1");
  }

  @Test
  void actionSimpleUpdateTest() {
    MockExporter mockExporter = new MockExporter();
    ProcessorConfig config = new ProcessorConfig();
    config.type = ProcessorType.ATTRIBUTE;
    config.id = "actionSimpleUpdate";
    ProcessorAction action = new ProcessorAction();
    action.key = "testKey";
    action.action = ProcessorActionType.UPDATE;
    action.value = "redacted";
    List<ProcessorAction> actions = new ArrayList<>();
    actions.add(action);
    config.actions = actions;
    SpanExporter exampleExporter = new ExporterWithAttributeProcessor(config, mockExporter);

    Span span =
        tracer
            .spanBuilder("my span")
            .setAttribute("one", "1")
            .setAttribute("two", 2L)
            .setAttribute("testKey", "testValue")
            .setAttribute("TESTKEY", "testValue2")
            .startSpan();

    Span log =
        tracer
            .spanBuilder("my log")
            .setAttribute("one", "1")
            .setAttribute("two", 2L)
            .setAttribute("testKey", "testValue")
            .setAttribute("TESTKEY", "testValue2")
            .setAttribute("applicationinsights.internal.log", true)
            .startSpan();

    SpanData spanData = ((ReadableSpan) span).toSpanData();
    SpanData logData = ((ReadableSpan) log).toSpanData();

    List<SpanData> spans = new ArrayList<>();
    spans.add(spanData);
    spans.add(logData);
    exampleExporter.export(spans);

    // verify that resulting spans are filtered in the way we want
    List<SpanData> result = mockExporter.getSpans();
    SpanData resultSpan = result.get(0);
    SpanData resultLog = result.get(1);

    assertThat(resultSpan.getAttributes().get(AttributeKey.stringKey("testKey")))
        .isEqualTo("redacted");
    assertThat(resultLog.getAttributes().get(AttributeKey.stringKey("testKey")))
        .isEqualTo("redacted");
  }

  @Test
  void actionUpdateFromAttributeUpdateTest() {
    MockExporter mockExporter = new MockExporter();
    ProcessorConfig config = new ProcessorConfig();
    config.type = ProcessorType.ATTRIBUTE;
    config.id = "actionUpdateFromAttributeUpdate";
    ProcessorAction action = new ProcessorAction();
    action.key = "testKey";
    action.action = ProcessorActionType.UPDATE;
    action.fromAttribute = "testKey2";
    List<ProcessorAction> actions = new ArrayList<>();
    actions.add(action);
    config.actions = actions;
    SpanExporter exampleExporter = new ExporterWithAttributeProcessor(config, mockExporter);

    Span span =
        tracer
            .spanBuilder("my span")
            .setAttribute("one", "1")
            .setAttribute("two", 2L)
            .setAttribute("testKey", "testValue")
            .setAttribute("testKey2", "testValue2")
            .startSpan();

    SpanData spanData = ((ReadableSpan) span).toSpanData();

    List<SpanData> spans = new ArrayList<>();
    spans.add(spanData);
    exampleExporter.export(spans);

    // verify that resulting spans are filtered in the way we want
    List<SpanData> result = mockExporter.getSpans();
    SpanData resultSpan = result.get(0);

    assertThat(resultSpan.getAttributes().get(AttributeKey.stringKey("testKey")))
        .isEqualTo("testValue2");
  }

  @Test
  void complexActionTest() {
    MockExporter mockExporter = new MockExporter();
    ProcessorConfig config = new ProcessorConfig();
    config.type = ProcessorType.ATTRIBUTE;
    config.id = "complexAction";
    ProcessorAction updateAction = new ProcessorAction();
    updateAction.key = "testKey";
    updateAction.action = ProcessorActionType.UPDATE;
    updateAction.value = "redacted";
    ProcessorAction deleteAction = new ProcessorAction();
    deleteAction.key = "testKey2";
    deleteAction.action = ProcessorActionType.DELETE;
    List<ProcessorAction> actions = new ArrayList<>();
    actions.add(updateAction);
    actions.add(deleteAction);
    config.actions = actions;
    SpanExporter exampleExporter = new ExporterWithAttributeProcessor(config, mockExporter);

    Span span =
        tracer
            .spanBuilder("my span")
            .setAttribute("one", "1")
            .setAttribute("two", 2L)
            .setAttribute("testKey", "testValue")
            .setAttribute("testKey2", "testValue2")
            .startSpan();

    SpanData spanData = ((ReadableSpan) span).toSpanData();

    List<SpanData> spans = new ArrayList<>();
    spans.add(spanData);
    exampleExporter.export(spans);

    // verify that resulting spans are filtered in the way we want
    List<SpanData> result = mockExporter.getSpans();
    SpanData resultSpan = result.get(0);

    assertThat(resultSpan.getAttributes().get(AttributeKey.stringKey("testKey")))
        .isEqualTo("redacted");
    assertThat(resultSpan.getAttributes().get(AttributeKey.stringKey("testKey2"))).isNull();
  }

  @Test
  void simpleIncludeTest() {
    MockExporter mockExporter = new MockExporter();
    ProcessorConfig config = new ProcessorConfig();
    config.type = ProcessorType.ATTRIBUTE;
    config.id = "simpleInclude";
    config.include = new ProcessorIncludeExclude();
    config.include.matchType = MatchType.STRICT;
    config.include.spanNames = Arrays.asList("svcA", "svcB");
    ProcessorAction action = new ProcessorAction();
    action.key = "testKey";
    action.action = ProcessorActionType.UPDATE;
    action.value = "redacted";
    List<ProcessorAction> actions = new ArrayList<>();
    actions.add(action);
    config.actions = actions;
    SpanExporter exampleExporter = new ExporterWithAttributeProcessor(config, mockExporter);

    Span spanA =
        tracer
            .spanBuilder("svcA")
            .setAttribute("one", "1")
            .setAttribute("two", 2L)
            .setAttribute("testKey", "testValue")
            .setAttribute("testKey2", "testValue2")
            .startSpan();
    Span spanB =
        tracer
            .spanBuilder("svcB")
            .setAttribute("one", "1")
            .setAttribute("testKey", "testValue")
            .setAttribute("testKey2", "testValue2")
            .startSpan();
    Span spanC =
        tracer
            .spanBuilder("svcC")
            .setAttribute("two", 2L)
            .setAttribute("testKey", "testValue")
            .setAttribute("testKey2", "testValue2")
            .startSpan();
    Span spanD =
        tracer
            .spanBuilder("svcD")
            .setAttribute("one", "1")
            .setAttribute("two", 2L)
            .setAttribute("testKey", "testValue")
            .setAttribute("testKey2", "testValue2")
            .startSpan();

    List<SpanData> spans = new ArrayList<>();
    spans.add(((ReadableSpan) spanA).toSpanData());
    spans.add(((ReadableSpan) spanB).toSpanData());
    spans.add(((ReadableSpan) spanC).toSpanData());
    spans.add(((ReadableSpan) spanD).toSpanData());

    exampleExporter.export(spans);

    // verify that resulting spans are filtered in the way we want
    List<SpanData> result = mockExporter.getSpans();
    SpanData resultSpanA = result.get(0);
    SpanData resultSpanB = result.get(1);
    SpanData resultSpanC = result.get(2);

    assertThat(resultSpanA.getAttributes().get(AttributeKey.stringKey("testKey")))
        .isEqualTo("redacted");
    assertThat(resultSpanB.getAttributes().get(AttributeKey.stringKey("testKey")))
        .isEqualTo("redacted");
    assertThat(resultSpanC.getAttributes().get(AttributeKey.stringKey("testKey")))
        .isEqualTo("testValue");
  }

  @Test
  void simpleIncludeWithSpanNamesTest() {
    MockExporter mockExporter = new MockExporter();
    ProcessorConfig config = new ProcessorConfig();
    config.type = ProcessorType.ATTRIBUTE;
    config.id = "simpleIncludeWithSpanNames";
    config.include = new ProcessorIncludeExclude();
    config.include.matchType = MatchType.STRICT;
    config.include.spanNames = Arrays.asList("svcA", "svcB");
    ProcessorAction action = new ProcessorAction();
    action.key = "testKey";
    action.action = ProcessorActionType.UPDATE;
    action.value = "redacted";
    List<ProcessorAction> actions = new ArrayList<>();
    actions.add(action);
    config.actions = actions;
    SpanExporter exampleExporter = new ExporterWithAttributeProcessor(config, mockExporter);

    Span spanA =
        tracer
            .spanBuilder("svcA")
            .setAttribute("one", "1")
            .setAttribute("two", 2L)
            .setAttribute("testKey", "testValue")
            .setAttribute("testKey2", "testValue2")
            .startSpan();
    Span spanB =
        tracer
            .spanBuilder("svcB")
            .setAttribute("one", "1")
            .setAttribute("testKey", "testValue")
            .setAttribute("testKey2", "testValue2")
            .startSpan();
    Span spanC =
        tracer
            .spanBuilder("svcC")
            .setAttribute("two", 2L)
            .setAttribute("testKey", "testValue")
            .setAttribute("testKey2", "testValue2")
            .startSpan();
    Span spanD =
        tracer
            .spanBuilder("svcD")
            .setAttribute("one", "1")
            .setAttribute("two", 2L)
            .setAttribute("testKey", "testValue")
            .setAttribute("testKey2", "testValue2")
            .startSpan();
    Span logA =
        tracer
            .spanBuilder("svcA")
            .setAttribute("one", "1")
            .setAttribute("two", 2L)
            .setAttribute("testKey", "testValue")
            .setAttribute("testKey2", "testValue2")
            .setAttribute("applicationinsights.internal.log", true)
            .startSpan();

    List<SpanData> spans = new ArrayList<>();
    spans.add(((ReadableSpan) spanA).toSpanData());
    spans.add(((ReadableSpan) spanB).toSpanData());
    spans.add(((ReadableSpan) spanC).toSpanData());
    spans.add(((ReadableSpan) spanD).toSpanData());
    spans.add(((ReadableSpan) logA).toSpanData());

    exampleExporter.export(spans);

    // verify that resulting spans are filtered in the way we want
    List<SpanData> result = mockExporter.getSpans();
    SpanData resultSpanA = result.get(0);
    SpanData resultSpanB = result.get(1);
    SpanData resultSpanC = result.get(2);
    SpanData resultLogA = result.get(2);

    assertThat(resultSpanA.getAttributes().get(AttributeKey.stringKey("testKey")))
        .isEqualTo("redacted");
    assertThat(resultSpanB.getAttributes().get(AttributeKey.stringKey("testKey")))
        .isEqualTo("redacted");
    assertThat(resultSpanC.getAttributes().get(AttributeKey.stringKey("testKey")))
        .isEqualTo("testValue");
    // Make sure log is not updated, since we have spanNames in include criteria
    assertThat(resultLogA.getAttributes().get(AttributeKey.stringKey("testKey")))
        .isEqualTo("testValue");
  }

  @Test
  void simpleIncludeRegexTest() {
    MockExporter mockExporter = new MockExporter();
    ProcessorConfig config = new ProcessorConfig();
    config.type = ProcessorType.ATTRIBUTE;
    config.id = "simpleIncludeRegex";
    config.include = new ProcessorIncludeExclude();
    config.include.matchType = MatchType.REGEXP;
    config.include.spanNames = Arrays.asList("svc.*", "test.*");
    ProcessorAction action = new ProcessorAction();
    action.key = "testKey";
    action.action = ProcessorActionType.UPDATE;
    action.value = "redacted";
    List<ProcessorAction> actions = new ArrayList<>();
    actions.add(action);
    config.actions = actions;
    SpanExporter exampleExporter = new ExporterWithAttributeProcessor(config, mockExporter);

    Span spanA =
        tracer
            .spanBuilder("svcA")
            .setAttribute("one", "1")
            .setAttribute("two", 2L)
            .setAttribute("testKey", "testValue")
            .setAttribute("testKey2", "testValue2")
            .startSpan();
    Span spanB =
        tracer
            .spanBuilder("svcB")
            .setAttribute("one", "1")
            .setAttribute("testKey", "testValue")
            .setAttribute("testKey2", "testValue2")
            .startSpan();
    Span spanC =
        tracer
            .spanBuilder("serviceC")
            .setAttribute("two", 2L)
            .setAttribute("testKey", "testValue")
            .setAttribute("testKey2", "testValue2")
            .startSpan();
    Span spanD =
        tracer
            .spanBuilder("serviceD")
            .setAttribute("one", "1")
            .setAttribute("two", 2L)
            .setAttribute("testKey", "testValue")
            .setAttribute("testKey2", "testValue2")
            .startSpan();

    List<SpanData> spans = new ArrayList<>();
    spans.add(((ReadableSpan) spanA).toSpanData());
    spans.add(((ReadableSpan) spanB).toSpanData());
    spans.add(((ReadableSpan) spanC).toSpanData());
    spans.add(((ReadableSpan) spanD).toSpanData());

    exampleExporter.export(spans);

    // verify that resulting spans are filtered in the way we want
    List<SpanData> result = mockExporter.getSpans();
    SpanData resultSpanA = result.get(0);
    SpanData resultSpanB = result.get(1);
    SpanData resultSpanC = result.get(2);

    assertThat(resultSpanA.getAttributes().get(AttributeKey.stringKey("testKey")))
        .isEqualTo("redacted");
    assertThat(resultSpanB.getAttributes().get(AttributeKey.stringKey("testKey")))
        .isEqualTo("redacted");
    assertThat(resultSpanC.getAttributes().get(AttributeKey.stringKey("testKey")))
        .isEqualTo("testValue");
  }

  @Test
  void invalidRegexTest() {
    MockExporter mockExporter = new MockExporter();
    ProcessorConfig config = new ProcessorConfig();
    config.type = ProcessorType.ATTRIBUTE;
    config.id = "invalidRegex";
    config.include = new ProcessorIncludeExclude();
    config.include.matchType = MatchType.REGEXP;
    config.include.spanNames = Collections.singletonList("***");
    ProcessorAction action = new ProcessorAction();
    action.key = "testKey";
    action.action = ProcessorActionType.UPDATE;
    action.value = "redacted";
    List<ProcessorAction> actions = new ArrayList<>();
    actions.add(action);
    config.actions = actions;

    assertThatThrownBy(() -> new ExporterWithAttributeProcessor(config, mockExporter))
        .isInstanceOf(FriendlyException.class);
  }

  @Test
  void simpleIncludeRegexValueTest() {
    MockExporter mockExporter = new MockExporter();
    ProcessorConfig config = new ProcessorConfig();
    config.type = ProcessorType.ATTRIBUTE;
    config.id = "simpleIncludeRegexValue";
    config.include = new ProcessorIncludeExclude();
    config.include.matchType = MatchType.REGEXP;
    config.include.spanNames = Arrays.asList("svc.*", "test.*");
    ProcessorAttribute attributeWithValue = new ProcessorAttribute();
    attributeWithValue.key = "testKey";
    attributeWithValue.value = "Value.*";
    config.include.attributes = new ArrayList<>();
    config.include.attributes.add(attributeWithValue);
    ProcessorAction action = new ProcessorAction();
    action.key = "testKey";
    action.action = ProcessorActionType.UPDATE;
    action.value = "redacted";
    List<ProcessorAction> actions = new ArrayList<>();
    actions.add(action);
    config.actions = actions;
    SpanExporter exampleExporter = new ExporterWithAttributeProcessor(config, mockExporter);

    Span spanA =
        tracer
            .spanBuilder("svcA")
            .setAttribute("one", "1")
            .setAttribute("two", 2L)
            .setAttribute("testKey", "testValue1")
            .setAttribute("testKey2", "testValue2")
            .startSpan();
    Span spanB =
        tracer
            .spanBuilder("svcB")
            .setAttribute("one", "1")
            .setAttribute("testKey", "testValue2")
            .setAttribute("testKey2", "testValue2")
            .startSpan();
    Span spanC =
        tracer
            .spanBuilder("serviceC")
            .setAttribute("two", 2L)
            .setAttribute("testKey", "testValue")
            .setAttribute("testKey2", "testValue2")
            .startSpan();
    Span spanD =
        tracer
            .spanBuilder("serviceD")
            .setAttribute("one", "1")
            .setAttribute("two", 2L)
            .setAttribute("testKey", "testValue")
            .setAttribute("testKey2", "testValue2")
            .startSpan();
    Span spanE =
        tracer
            .spanBuilder("svcE")
            .setAttribute("one", "1")
            .setAttribute("two", 2L)
            .setAttribute("testKey", "testV1")
            .setAttribute("testKey2", "testValue2")
            .startSpan();
    List<SpanData> spans = new ArrayList<>();
    spans.add(((ReadableSpan) spanA).toSpanData());
    spans.add(((ReadableSpan) spanB).toSpanData());
    spans.add(((ReadableSpan) spanC).toSpanData());
    spans.add(((ReadableSpan) spanD).toSpanData());
    spans.add(((ReadableSpan) spanE).toSpanData());

    exampleExporter.export(spans);

    // verify that resulting spans are filtered in the way we want
    List<SpanData> result = mockExporter.getSpans();
    SpanData resultSpanA = result.get(0);
    SpanData resultSpanB = result.get(1);
    SpanData resultSpanC = result.get(2);
    SpanData resultSpanE = result.get(4);

    assertThat(resultSpanA.getAttributes().get(AttributeKey.stringKey("testKey")))
        .isEqualTo("redacted");
    assertThat(resultSpanB.getAttributes().get(AttributeKey.stringKey("testKey")))
        .isEqualTo("redacted");
    assertThat(resultSpanC.getAttributes().get(AttributeKey.stringKey("testKey")))
        .isEqualTo("testValue");
    assertThat(resultSpanE.getAttributes().get(AttributeKey.stringKey("testKey")))
        .isEqualTo("testV1");
  }

  @Test
  void simpleIncludeRegexNoValueTest() {
    MockExporter mockExporter = new MockExporter();
    ProcessorConfig config = new ProcessorConfig();
    config.type = ProcessorType.ATTRIBUTE;
    config.id = "simpleIncludeRegexNoValue";
    config.include = new ProcessorIncludeExclude();
    config.include.matchType = MatchType.REGEXP;
    config.include.spanNames = Arrays.asList("svc.*", "test.*");
    ProcessorAttribute attributeWithNoValue = new ProcessorAttribute();
    attributeWithNoValue.key = "testKey";
    config.include.attributes = new ArrayList<>();
    config.include.attributes.add(attributeWithNoValue);
    ProcessorAction action = new ProcessorAction();
    action.key = "testKey";
    action.action = ProcessorActionType.UPDATE;
    action.value = "redacted";
    List<ProcessorAction> actions = new ArrayList<>();
    actions.add(action);
    config.actions = actions;
    SpanExporter exampleExporter = new ExporterWithAttributeProcessor(config, mockExporter);

    Span spanA =
        tracer
            .spanBuilder("svcA")
            .setAttribute("one", "1")
            .setAttribute("two", 2L)
            .setAttribute("testKey", "testValue1")
            .setAttribute("testKey2", "testValue2")
            .startSpan();
    Span spanB =
        tracer
            .spanBuilder("svcB")
            .setAttribute("one", "1")
            .setAttribute("testKey", "testValue2")
            .setAttribute("testKey2", "testValue2")
            .startSpan();
    Span spanC =
        tracer
            .spanBuilder("serviceC")
            .setAttribute("two", 2L)
            .setAttribute("testKey", "testValue")
            .setAttribute("testKey2", "testValue2")
            .startSpan();
    Span spanD =
        tracer
            .spanBuilder("serviceD")
            .setAttribute("one", "1")
            .setAttribute("two", 2L)
            .setAttribute("testKey", "testValue")
            .setAttribute("testKey2", "testValue2")
            .startSpan();
    Span spanE =
        tracer
            .spanBuilder("svcE")
            .setAttribute("one", "1")
            .setAttribute("two", 2L)
            .setAttribute("testKey", "testV1")
            .setAttribute("testKey2", "testValue2")
            .startSpan();
    List<SpanData> spans = new ArrayList<>();
    spans.add(((ReadableSpan) spanA).toSpanData());
    spans.add(((ReadableSpan) spanB).toSpanData());
    spans.add(((ReadableSpan) spanC).toSpanData());
    spans.add(((ReadableSpan) spanD).toSpanData());
    spans.add(((ReadableSpan) spanE).toSpanData());

    exampleExporter.export(spans);

    // verify that resulting spans are filtered in the way we want
    List<SpanData> result = mockExporter.getSpans();
    SpanData resultSpanA = result.get(0);
    SpanData resultSpanB = result.get(1);
    SpanData resultSpanC = result.get(2);
    SpanData resultSpanE = result.get(4);

    assertThat(resultSpanA.getAttributes().get(AttributeKey.stringKey("testKey")))
        .isEqualTo("redacted");
    assertThat(resultSpanB.getAttributes().get(AttributeKey.stringKey("testKey")))
        .isEqualTo("redacted");
    assertThat(resultSpanC.getAttributes().get(AttributeKey.stringKey("testKey")))
        .isEqualTo("testValue");
    assertThat(resultSpanE.getAttributes().get(AttributeKey.stringKey("testKey")))
        .isEqualTo("redacted");
  }

  @Test
  void simpleIncludeHashTest() {
    MockExporter mockExporter = new MockExporter();
    ProcessorConfig config = new ProcessorConfig();
    config.type = ProcessorType.ATTRIBUTE;
    config.id = "simpleIncludeHash";
    config.include = new ProcessorIncludeExclude();
    config.include.matchType = MatchType.STRICT;
    config.include.spanNames = Arrays.asList("svcA", "svcB", "svcC");
    ProcessorAction action = new ProcessorAction();
    action.key = "testKey";
    action.action = ProcessorActionType.HASH;
    List<ProcessorAction> actions = new ArrayList<>();
    actions.add(action);
    config.actions = actions;
    SpanExporter exampleExporter = new ExporterWithAttributeProcessor(config, mockExporter);

    Span spanA =
        tracer
            .spanBuilder("svcA")
            .setAttribute("one", "1")
            .setAttribute("two", 2L)
            .setAttribute("testKey", "testValue")
            .setAttribute("testKey2", "testValue2")
            .startSpan();
    Span spanB =
        tracer
            .spanBuilder("svcB")
            .setAttribute("one", "1")
            .setAttribute("testKey", 2L)
            .setAttribute("testKey2", "testValue2")
            .startSpan();
    Span spanC =
        tracer
            .spanBuilder("svcC")
            .setAttribute("two", 2L)
            .setAttribute("testKey", 123)
            .setAttribute("testKey2", "testValue2")
            .startSpan();
    Span spanD =
        tracer
            .spanBuilder("svcD")
            .setAttribute("one", "1")
            .setAttribute("two", 2L)
            .setAttribute("testKey", "testValue")
            .setAttribute("testKey2", "testValue2")
            .startSpan();

    List<SpanData> spans = new ArrayList<>();
    spans.add(((ReadableSpan) spanA).toSpanData());
    spans.add(((ReadableSpan) spanB).toSpanData());
    spans.add(((ReadableSpan) spanC).toSpanData());
    spans.add(((ReadableSpan) spanD).toSpanData());

    exampleExporter.export(spans);

    // verify that resulting spans are filtered in the way we want
    List<SpanData> result = mockExporter.getSpans();
    SpanData resultSpanA = result.get(0);
    SpanData resultSpanB = result.get(1);
    SpanData resultSpanD = result.get(3);

    assertThat(resultSpanA.getAttributes().get(AttributeKey.stringKey("testKey")))
        .isNotEqualTo("testValue");
    assertThat(resultSpanB.getAttributes().get(AttributeKey.stringKey("testKey2")))
        .isEqualTo("testValue2");
    assertThat(resultSpanD.getAttributes().get(AttributeKey.stringKey("testKey")))
        .isEqualTo("testValue");
  }

  @Test
  void simpleExcludeTest() {
    MockExporter mockExporter = new MockExporter();
    ProcessorConfig config = new ProcessorConfig();
    config.type = ProcessorType.ATTRIBUTE;
    config.id = "simpleExclude";
    config.exclude = new ProcessorIncludeExclude();
    config.exclude.matchType = MatchType.STRICT;
    config.exclude.spanNames = Arrays.asList("svcA", "svcB");
    ProcessorAction action = new ProcessorAction();
    action.key = "testKey";
    action.action = ProcessorActionType.UPDATE;
    action.value = "redacted";
    List<ProcessorAction> actions = new ArrayList<>();
    actions.add(action);
    config.actions = actions;
    SpanExporter exampleExporter = new ExporterWithAttributeProcessor(config, mockExporter);

    Span spanA =
        tracer
            .spanBuilder("svcA")
            .setAttribute("one", "1")
            .setAttribute("two", 2L)
            .setAttribute("testKey", "testValue")
            .setAttribute("testKey2", "testValue2")
            .startSpan();
    Span spanB =
        tracer
            .spanBuilder("svcB")
            .setAttribute("one", "1")
            .setAttribute("testKey", "testValue")
            .setAttribute("testKey2", "testValue2")
            .startSpan();
    Span spanC =
        tracer
            .spanBuilder("svcC")
            .setAttribute("two", 2L)
            .setAttribute("testKey", "testValue")
            .setAttribute("testKey2", "testValue2")
            .startSpan();
    Span spanD =
        tracer
            .spanBuilder("svcD")
            .setAttribute("one", "1")
            .setAttribute("two", 2L)
            .setAttribute("testKey", "testValue")
            .setAttribute("testKey2", "testValue2")
            .startSpan();

    List<SpanData> spans = new ArrayList<>();
    spans.add(((ReadableSpan) spanA).toSpanData());
    spans.add(((ReadableSpan) spanB).toSpanData());
    spans.add(((ReadableSpan) spanC).toSpanData());
    spans.add(((ReadableSpan) spanD).toSpanData());

    exampleExporter.export(spans);

    // verify that resulting spans are filtered in the way we want
    List<SpanData> result = mockExporter.getSpans();
    SpanData resultSpanA = result.get(0);
    SpanData resultSpanB = result.get(1);
    SpanData resultSpanC = result.get(2);

    assertThat(resultSpanA.getAttributes().get(AttributeKey.stringKey("testKey")))
        .isEqualTo("testValue");
    assertThat(resultSpanB.getAttributes().get(AttributeKey.stringKey("testKey")))
        .isEqualTo("testValue");
    assertThat(resultSpanC.getAttributes().get(AttributeKey.stringKey("testKey")))
        .isEqualTo("redacted");
  }

  @Test
  void simpleExcludeRegexTest() {
    MockExporter mockExporter = new MockExporter();
    ProcessorConfig config = new ProcessorConfig();
    config.type = ProcessorType.ATTRIBUTE;
    config.id = "simpleExcludeRegex";
    config.exclude = new ProcessorIncludeExclude();
    config.exclude.matchType = MatchType.REGEXP;
    config.exclude.spanNames = Collections.singletonList("svc.*");
    ProcessorAction action = new ProcessorAction();
    action.key = "testKey";
    action.action = ProcessorActionType.UPDATE;
    action.value = "redacted";
    List<ProcessorAction> actions = new ArrayList<>();
    actions.add(action);
    config.actions = actions;
    SpanExporter exampleExporter = new ExporterWithAttributeProcessor(config, mockExporter);

    Span spanA =
        tracer
            .spanBuilder("svcA")
            .setAttribute("one", "1")
            .setAttribute("two", 2L)
            .setAttribute("testKey", "testValue")
            .setAttribute("testKey2", "testValue2")
            .startSpan();
    Span spanB =
        tracer
            .spanBuilder("svcB")
            .setAttribute("one", "1")
            .setAttribute("testKey", "testValue")
            .setAttribute("testKey2", "testValue2")
            .startSpan();
    Span spanC =
        tracer
            .spanBuilder("serviceC")
            .setAttribute("two", 2L)
            .setAttribute("testKey", "testValue")
            .setAttribute("testKey2", "testValue2")
            .startSpan();
    Span spanD =
        tracer
            .spanBuilder("serviceD")
            .setAttribute("one", "1")
            .setAttribute("two", 2L)
            .setAttribute("testKey", "testValue")
            .setAttribute("testKey2", "testValue2")
            .startSpan();

    List<SpanData> spans = new ArrayList<>();
    spans.add(((ReadableSpan) spanA).toSpanData());
    spans.add(((ReadableSpan) spanB).toSpanData());
    spans.add(((ReadableSpan) spanC).toSpanData());
    spans.add(((ReadableSpan) spanD).toSpanData());

    exampleExporter.export(spans);

    // verify that resulting spans are filtered in the way we want
    List<SpanData> result = mockExporter.getSpans();
    SpanData resultSpanA = result.get(0);
    SpanData resultSpanB = result.get(1);
    SpanData resultSpanC = result.get(2);
    SpanData resultSpanD = result.get(3);

    assertThat(resultSpanA.getAttributes().get(AttributeKey.stringKey("testKey")))
        .isEqualTo("testValue");
    assertThat(resultSpanB.getAttributes().get(AttributeKey.stringKey("testKey")))
        .isEqualTo("testValue");
    assertThat(resultSpanC.getAttributes().get(AttributeKey.stringKey("testKey")))
        .isEqualTo("redacted");
    assertThat(resultSpanD.getAttributes().get(AttributeKey.stringKey("testKey")))
        .isEqualTo("redacted");
  }

  @Test
  void multiIncludeTest() {
    MockExporter mockExporter = new MockExporter();
    ProcessorConfig config = new ProcessorConfig();
    config.type = ProcessorType.ATTRIBUTE;
    config.id = "multiInclude";
    config.include = new ProcessorIncludeExclude();
    config.include.matchType = MatchType.STRICT;
    config.include.spanNames = Arrays.asList("svcA", "svcB");
    config.include.attributes = new ArrayList<>();
    ProcessorAttribute attributeWithValue = new ProcessorAttribute();
    attributeWithValue.key = "testKey";
    attributeWithValue.value = "testValue";
    ProcessorAttribute attributeWithNoValue = new ProcessorAttribute();
    attributeWithNoValue.key = "testKey2";
    config.include.attributes.add(attributeWithValue);
    config.include.attributes.add(attributeWithNoValue);
    ProcessorAction action = new ProcessorAction();
    action.key = "testKey";
    action.action = ProcessorActionType.DELETE;
    List<ProcessorAction> actions = new ArrayList<>();
    actions.add(action);
    config.actions = actions;
    SpanExporter exampleExporter = new ExporterWithAttributeProcessor(config, mockExporter);

    Span spanA =
        tracer
            .spanBuilder("svcA")
            .setAttribute("one", "1")
            .setAttribute("two", 2L)
            .setAttribute("testKey", "testValue")
            .setAttribute("testKey2", "testValue2")
            .startSpan();
    Span spanB =
        tracer
            .spanBuilder("svcB")
            .setAttribute("one", "1")
            .setAttribute("testKey", "testValue")
            .setAttribute("testKey3", "testValue3")
            .startSpan();
    Span spanC =
        tracer
            .spanBuilder("svcC")
            .setAttribute("two", 2L)
            .setAttribute("testKey", "testValue")
            .setAttribute("testKey2", "testValue2")
            .startSpan();
    Span spanD =
        tracer
            .spanBuilder("svcD")
            .setAttribute("one", "1")
            .setAttribute("two", 2L)
            .setAttribute("testKey", "testValue")
            .setAttribute("testKey2", "testValue2")
            .startSpan();

    List<SpanData> spans = new ArrayList<>();
    spans.add(((ReadableSpan) spanA).toSpanData());
    spans.add(((ReadableSpan) spanB).toSpanData());
    spans.add(((ReadableSpan) spanC).toSpanData());
    spans.add(((ReadableSpan) spanD).toSpanData());

    exampleExporter.export(spans);

    // verify that resulting spans are filtered in the way we want
    List<SpanData> result = mockExporter.getSpans();
    SpanData resultSpanA = result.get(0);
    SpanData resultSpanB = result.get(1);
    SpanData resultSpanC = result.get(2);

    assertThat(resultSpanA.getAttributes().get(AttributeKey.stringKey("testKey"))).isNull();
    assertThat(resultSpanB.getAttributes().get(AttributeKey.stringKey("testKey")))
        .isEqualTo("testValue");
    assertThat(resultSpanC.getAttributes().get(AttributeKey.stringKey("testKey")))
        .isEqualTo("testValue");
  }

  @Test
  void multiExcludeTest() {
    MockExporter mockExporter = new MockExporter();
    ProcessorConfig config = new ProcessorConfig();
    config.type = ProcessorType.ATTRIBUTE;
    config.id = "multiExclude";
    config.exclude = new ProcessorIncludeExclude();
    config.exclude.matchType = MatchType.STRICT;
    config.exclude.spanNames = Arrays.asList("svcA", "svcB");
    config.exclude.attributes = new ArrayList<>();
    ProcessorAttribute attributeWithValue = new ProcessorAttribute();
    attributeWithValue.key = "testKey";
    attributeWithValue.value = "testValue";
    config.exclude.attributes.add(attributeWithValue);
    ProcessorAttribute attributeWithNoValue = new ProcessorAttribute();
    attributeWithNoValue.key = "testKey2";
    config.exclude.attributes.add(attributeWithNoValue);
    ProcessorAction action = new ProcessorAction();
    action.key = "testKey";
    action.action = ProcessorActionType.DELETE;
    List<ProcessorAction> actions = new ArrayList<>();
    actions.add(action);
    config.actions = actions;
    SpanExporter exampleExporter = new ExporterWithAttributeProcessor(config, mockExporter);

    Span spanA =
        tracer
            .spanBuilder("svcA")
            .setAttribute("one", "1")
            .setAttribute("two", 2L)
            .setAttribute("testKey", "testValue")
            .setAttribute("testKey2", "testValue2")
            .startSpan();
    Span spanB =
        tracer
            .spanBuilder("svcB")
            .setAttribute("one", "1")
            .setAttribute("testKey", "testValue")
            .setAttribute("testKey3", "testValue2")
            .startSpan();
    Span spanC =
        tracer
            .spanBuilder("svcC")
            .setAttribute("two", 2L)
            .setAttribute("testKey", "testValue")
            .setAttribute("testKey2", "testValue2")
            .startSpan();
    Span spanD =
        tracer
            .spanBuilder("svcD")
            .setAttribute("one", "1")
            .setAttribute("two", 2L)
            .setAttribute("testKey", "testValue")
            .setAttribute("testKey2", "testValue2")
            .startSpan();

    List<SpanData> spans = new ArrayList<>();
    spans.add(((ReadableSpan) spanA).toSpanData());
    spans.add(((ReadableSpan) spanB).toSpanData());
    spans.add(((ReadableSpan) spanC).toSpanData());
    spans.add(((ReadableSpan) spanD).toSpanData());

    exampleExporter.export(spans);

    // verify that resulting spans are filtered in the way we want
    List<SpanData> result = mockExporter.getSpans();
    SpanData resultSpanA = result.get(0);
    SpanData resultSpanB = result.get(1);
    SpanData resultSpanC = result.get(2);
    SpanData resultSpanD = result.get(3);

    assertThat(resultSpanA.getAttributes().get(AttributeKey.stringKey("testKey")))
        .isEqualTo("testValue");
    assertThat(resultSpanB.getAttributes().get(AttributeKey.stringKey("testKey"))).isNull();
    assertThat(resultSpanC.getAttributes().get(AttributeKey.stringKey("testKey"))).isNull();
    assertThat(resultSpanD.getAttributes().get(AttributeKey.stringKey("testKey"))).isNull();
  }

  @Test
  void selectiveProcessingTest() { // With both include and exclude
    MockExporter mockExporter = new MockExporter();
    ProcessorConfig config = new ProcessorConfig();
    config.type = ProcessorType.ATTRIBUTE;
    config.id = "selectiveProcessing";
    config.include = new ProcessorIncludeExclude();
    config.exclude = new ProcessorIncludeExclude();
    config.include.matchType = MatchType.STRICT;
    config.include.spanNames = Arrays.asList("svcA", "svcB");
    config.exclude.matchType = MatchType.STRICT;
    config.exclude.attributes = new ArrayList<>();
    ProcessorAttribute attributeWithValue = new ProcessorAttribute();
    attributeWithValue.key = "testKey";
    attributeWithValue.value = "testValue";
    config.exclude.attributes.add(attributeWithValue);
    ProcessorAction action = new ProcessorAction();
    action.key = "testKey2";
    action.action = ProcessorActionType.DELETE;
    List<ProcessorAction> actions = new ArrayList<>();
    actions.add(action);
    config.actions = actions;
    SpanExporter exampleExporter = new ExporterWithAttributeProcessor(config, mockExporter);

    Span spanA =
        tracer
            .spanBuilder("svcA")
            .setAttribute("one", "1")
            .setAttribute("two", 2L)
            .setAttribute("testKey", "testValue")
            .setAttribute("testKey2", "testValue2")
            .startSpan();
    Span spanB =
        tracer
            .spanBuilder("svcB")
            .setAttribute("one", "1")
            .setAttribute("testKey", "testValue1")
            .setAttribute("testKey2", "testValue2")
            .startSpan();
    Span spanC =
        tracer
            .spanBuilder("svcC")
            .setAttribute("two", 2L)
            .setAttribute("testKey", "testValue")
            .setAttribute("testKey2", "testValue2")
            .startSpan();
    Span spanD =
        tracer
            .spanBuilder("svcD")
            .setAttribute("one", "1")
            .setAttribute("two", 2L)
            .setAttribute("testKey", "testValue")
            .setAttribute("testKey2", "testValue2")
            .startSpan();

    List<SpanData> spans = new ArrayList<>();
    spans.add(((ReadableSpan) spanA).toSpanData());
    spans.add(((ReadableSpan) spanB).toSpanData());
    spans.add(((ReadableSpan) spanC).toSpanData());
    spans.add(((ReadableSpan) spanD).toSpanData());

    exampleExporter.export(spans);

    // verify that resulting spans are filtered in the way we want
    List<SpanData> result = mockExporter.getSpans();
    SpanData resultSpanA = result.get(0);
    SpanData resultSpanB = result.get(1);
    SpanData resultSpanC = result.get(2);

    assertThat(resultSpanA.getAttributes().get(AttributeKey.stringKey("testKey2")))
        .isEqualTo("testValue2");
    assertThat(resultSpanB.getAttributes().get(AttributeKey.stringKey("testKey2"))).isNull();
    assertThat(resultSpanC.getAttributes().get(AttributeKey.stringKey("testKey2")))
        .isEqualTo("testValue2");
  }

  @Test
  void actionInsertWithExtractTest() {
    MockExporter mockExporter = new MockExporter();
    ProcessorConfig config = new ProcessorConfig();
    config.type = ProcessorType.ATTRIBUTE;
    config.id = "actionExtract";
    ProcessorAction action = new ProcessorAction();
    action.key = "testKey";
    String regex =
        "^(?<httpProtocol>.*)://(?<httpDomain>.*)/(?<httpPath>.*)([?&])(?<httpQueryParams>.*)";
    Pattern pattern = Pattern.compile(regex);
    List<String> groupNames = ProcessorActionAdaptor.getGroupNames(regex);
    action.extractAttribute = new ExtractAttribute(pattern, groupNames);
    action.action = ProcessorActionType.EXTRACT;
    List<ProcessorAction> actions = new ArrayList<>();
    actions.add(action);
    config.actions = actions;
    SpanExporter exampleExporter = new ExporterWithAttributeProcessor(config, mockExporter);

    Span span =
        tracer
            .spanBuilder("my span")
            .setAttribute("one", "1")
            .setAttribute("two", 2L)
            .setAttribute(
                "testKey", "http://example.com/path?queryParam1=value1,queryParam2=value2")
            .setAttribute("TESTKEY", "testValue2")
            .startSpan();

    SpanData spanData = ((ReadableSpan) span).toSpanData();

    List<SpanData> spans = new ArrayList<>();
    spans.add(spanData);
    exampleExporter.export(spans);

    // verify that resulting spans are filtered in the way we want
    List<SpanData> result = mockExporter.getSpans();
    SpanData resultSpan = result.get(0);

    assertThat(resultSpan.getAttributes().get(AttributeKey.stringKey("httpProtocol")))
        .isEqualTo("http");
    assertThat(resultSpan.getAttributes().get(AttributeKey.stringKey("httpDomain")))
        .isEqualTo("example.com");
    assertThat(resultSpan.getAttributes().get(AttributeKey.stringKey("httpPath")))
        .isEqualTo("path");
    assertThat(resultSpan.getAttributes().get(AttributeKey.stringKey("httpQueryParams")))
        .isEqualTo("queryParam1=value1,queryParam2=value2");
    assertThat(resultSpan.getAttributes().get(AttributeKey.stringKey("testKey")))
        .isEqualTo("http://example.com/path?queryParam1=value1,queryParam2=value2");
  }

  @Test
  void actionInsertWithExtractDuplicateTest() {
    MockExporter mockExporter = new MockExporter();
    ProcessorConfig config = new ProcessorConfig();
    config.type = ProcessorType.ATTRIBUTE;
    config.id = "actionExtract";
    ProcessorAction action = new ProcessorAction();
    action.key = "testKey";
    String regex =
        "^(?<httpProtocol>.*)://(?<httpDomain>.*)/(?<httpPath>.*)([?&])(?<httpQueryParams>.*)";
    Pattern pattern = Pattern.compile(regex);
    List<String> groupNames = ProcessorActionAdaptor.getGroupNames(regex);
    action.extractAttribute = new ExtractAttribute(pattern, groupNames);
    action.action = ProcessorActionType.EXTRACT;
    List<ProcessorAction> actions = new ArrayList<>();
    actions.add(action);
    config.actions = actions;
    SpanExporter exampleExporter = new ExporterWithAttributeProcessor(config, mockExporter);

    Span span =
        tracer
            .spanBuilder("my span")
            .setAttribute("one", "1")
            .setAttribute("two", 2L)
            .setAttribute(
                "testKey", "http://example.com/path?queryParam1=value1,queryParam2=value2")
            .setAttribute("httpPath", "oldPath")
            .startSpan();

    SpanData spanData = ((ReadableSpan) span).toSpanData();

    List<SpanData> spans = new ArrayList<>();
    spans.add(spanData);
    exampleExporter.export(spans);

    // verify that resulting spans are filtered in the way we want
    List<SpanData> result = mockExporter.getSpans();
    SpanData resultSpan = result.get(0);

    assertThat(resultSpan.getAttributes().get(AttributeKey.stringKey("httpProtocol")))
        .isEqualTo("http");
    assertThat(resultSpan.getAttributes().get(AttributeKey.stringKey("httpDomain")))
        .isEqualTo("example.com");
    assertThat(resultSpan.getAttributes().get(AttributeKey.stringKey("httpPath")))
        .isEqualTo("path");
    assertThat(resultSpan.getAttributes().get(AttributeKey.stringKey("httpPath")))
        .isNotEqualTo("oldPath");
    assertThat(resultSpan.getAttributes().get(AttributeKey.stringKey("httpQueryParams")))
        .isEqualTo("queryParam1=value1,queryParam2=value2");
    assertThat(resultSpan.getAttributes().get(AttributeKey.stringKey("testKey")))
        .isEqualTo("http://example.com/path?queryParam1=value1,queryParam2=value2");
  }
}
