package com.yupi.springbootinit.model.dto.chart;

import com.yupi.springbootinit.model.enums.ChartTaskPhaseEnum;
import lombok.Getter;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 图表任务 Agent 执行上下文（最小骨架）
 */
@Getter
public class ChartAgentExecutionContext {

    private static final DateTimeFormatter TRACE_TIME_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final long chartId;

    private final String executionId;

    private ChartTaskPhaseEnum currentPhase;

    private final List<TraceNode> traceList = new ArrayList<>();

    private ChartAgentExecutionContext(long chartId, String executionId) {
        this.chartId = chartId;
        this.executionId = executionId;
    }

    public static ChartAgentExecutionContext create(long chartId) {
        String executionId = UUID.randomUUID().toString().replace("-", "");
        ChartAgentExecutionContext context = new ChartAgentExecutionContext(chartId, executionId);
        context.markPhase(ChartTaskPhaseEnum.CREATED, "context_initialized");
        return context;
    }

    public void markPhase(ChartTaskPhaseEnum phase, String detail) {
        this.currentPhase = phase;
        String normalizedDetail = detail == null ? "" : detail.trim();
        this.traceList.add(new TraceNode(LocalDateTime.now().format(TRACE_TIME_FORMATTER), phase.getValue(), normalizedDetail));
    }

    public String buildPhasePath() {
        return traceList.stream().map(TraceNode::getPhase).collect(Collectors.joining(">"));
    }

    public String buildContextFragment() {
        return String.format("agentExecId=%s | agentPhase=%s | agentTrace=%s",
                executionId,
                currentPhase == null ? "unknown" : currentPhase.getValue(),
                buildPhasePath());
    }

    @Getter
    private static class TraceNode {
        private final String timestamp;
        private final String phase;
        private final String detail;

        private TraceNode(String timestamp, String phase, String detail) {
            this.timestamp = timestamp;
            this.phase = phase;
            this.detail = detail;
        }
    }
}
