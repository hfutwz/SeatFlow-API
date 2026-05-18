package com.seatflow.service.assistant;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.seatflow.config.AssistantProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Function Calling 工具定义与执行
 * <p>
 * 定义 LLM 可调用的系统工具（查询自习室、预约、取消等），
 * LLM 返回 tool_call 时，由本类执行并返回结果。
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class FunctionExecutor {

    private final AssistantProperties properties;
    private final ObjectMapper objectMapper;
    private final AssistantToolRunner toolRunner;

    /**
     * 获取所有工具定义（OpenAI Function Calling 格式）
     */
    public List<LlmModels.FunctionDef> getToolDefinitions() {
        return List.of(
                LlmModels.FunctionDef.builder()
                        .type("function")
                        .function(LlmModels.FunctionSchema.builder()
                                .name("query_rooms")
                                .description("查询开放的自习室列表，包括名称、位置、开放时间、空座数")
                                .parameters(Map.of(
                                        "type", "object",
                                        "properties", Map.of(),
                                        "required", List.of()
                                ))
                                .build())
                        .build(),
                LlmModels.FunctionDef.builder()
                        .type("function")
                        .function(LlmModels.FunctionSchema.builder()
                                .name("search_available_seats")
                                .description("搜索指定日期和时段的可用座位，支持按自习室、插座类型、位置等条件筛选")
                                .parameters(Map.of(
                                        "type", "object",
                                        "properties", Map.of(
                                                "date", Map.of("type", "string", "description", "日期，格式 YYYY-MM-DD"),
                                                "start_time", Map.of("type", "string", "description", "开始时间，格式 HH:mm"),
                                                "end_time", Map.of("type", "string", "description", "结束时间，格式 HH:mm"),
                                                "room_id", Map.of("type", "number", "description", "自习室ID（可选）"),
                                                "socket_type", Map.of("type", "string", "description", "插座类型：NONE/FIXED/MOVABLE（可选）"),
                                                "position", Map.of("type", "string", "description", "位置：WINDOW/CORRIDOR/MIDDLE（可选）")
                                        ),
                                        "required", List.of("date", "start_time", "end_time")
                                ))
                                .build())
                        .build(),
                LlmModels.FunctionDef.builder()
                        .type("function")
                        .function(LlmModels.FunctionSchema.builder()
                                .name("query_my_reservations")
                                .description("查询当前用户的预约列表")
                                .parameters(Map.of(
                                        "type", "object",
                                        "properties", Map.of(
                                                "status", Map.of("type", "string", "description", "预约状态：PENDING/CHECKED_IN/ALL（可选，默认ALL）")
                                        ),
                                        "required", List.of()
                                ))
                                .build())
                        .build(),
                LlmModels.FunctionDef.builder()
                        .type("function")
                        .function(LlmModels.FunctionSchema.builder()
                                .name("make_reservation")
                                .description("为当前用户预约座位。需要指定座位ID、日期和时段。如果用户未指定具体座位，可先调用 search_available_seats 查找")
                                .parameters(Map.of(
                                        "type", "object",
                                        "properties", Map.of(
                                                "seat_id", Map.of("type", "number", "description", "座位ID"),
                                                "date", Map.of("type", "string", "description", "日期，格式 YYYY-MM-DD"),
                                                "start_time", Map.of("type", "string", "description", "开始时间，整点格式 HH:mm"),
                                                "end_time", Map.of("type", "string", "description", "结束时间，整点格式 HH:mm")
                                        ),
                                        "required", List.of("seat_id", "date", "start_time", "end_time")
                                ))
                                .build())
                        .build(),
                LlmModels.FunctionDef.builder()
                        .type("function")
                        .function(LlmModels.FunctionSchema.builder()
                                .name("cancel_reservation")
                                .description("取消当前用户的预约。如果用户有多个预约，只取消最近的那个")
                                .parameters(Map.of(
                                        "type", "object",
                                        "properties", Map.of(
                                                "reservation_id", Map.of("type", "number", "description", "预约ID（可选，不提供则取消最近的一个待签到预约）")
                                        ),
                                        "required", List.of()
                                ))
                                .build())
                        .build(),
                LlmModels.FunctionDef.builder()
                        .type("function")
                        .function(LlmModels.FunctionSchema.builder()
                                .name("query_my_violations")
                                .description("查询当前用户的违约记录")
                                .parameters(Map.of(
                                        "type", "object",
                                        "properties", Map.of(),
                                        "required", List.of()
                                ))
                                .build())
                        .build()
        );
    }

    /**
     * 执行 Function Call
     */
    public String execute(String functionName, String argumentsJson) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> args = objectMapper.readValue(argumentsJson, Map.class);
            log.info("Function call: {} args={}", functionName, args);

            String result = switch (functionName) {
                case "query_rooms" -> toolRunner.queryRooms();
                case "search_available_seats" -> toolRunner.searchAvailableSeats(args);
                case "query_my_reservations" -> toolRunner.queryMyReservations(args);
                case "make_reservation" -> toolRunner.makeReservation(args);
                case "cancel_reservation" -> toolRunner.cancelReservation(args);
                case "query_my_violations" -> toolRunner.queryMyViolations();
                default -> "未知工具: " + functionName;
            };

            log.info("Function result: {} -> {} chars", functionName, result.length());
            return result;
        } catch (Exception e) {
            log.error("Function execution error: {} {}", functionName, e.getMessage());
            return "工具调用失败: " + e.getMessage();
        }
    }
}
