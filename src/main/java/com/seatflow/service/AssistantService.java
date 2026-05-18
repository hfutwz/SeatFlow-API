package com.seatflow.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.seatflow.dto.request.ChatRequest;
import com.seatflow.dto.response.ChatResponse;
import com.seatflow.entity.Reservation;
import com.seatflow.entity.Room;
import com.seatflow.entity.Seat;
import com.seatflow.entity.User;
import com.seatflow.mapper.ReservationMapper;
import com.seatflow.mapper.RoomMapper;
import com.seatflow.mapper.SeatMapper;
import com.seatflow.mapper.UserMapper;
import com.seatflow.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class AssistantService {

    private final RoomMapper roomMapper;
    private final SeatMapper seatMapper;
    private final ReservationMapper reservationMapper;
    private final UserMapper userMapper;
    private final ReservationService reservationService;

    // 简单会话上下文存储
    private final Map<String, Map<String, String>> sessionContext = new ConcurrentHashMap<>();

    /**
     * 处理用户消息，返回智能回复
     */
    public ChatResponse chat(ChatRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) return ChatResponse.builder().reply("请先登录后再使用智能助手").intent("AUTH_REQUIRED").build();

        String sessionId = request.getSessionId() != null ? request.getSessionId() : UUID.randomUUID().toString();
        String message = request.getMessage().trim();
        Map<String, String> context = sessionContext.computeIfAbsent(sessionId, k -> new ConcurrentHashMap<>());

        // 意图识别
        String intent = recognizeIntent(message, context);
        String reply = handleIntent(intent, message, userId, context);

        // 清理过期会话（简单策略：保留最近100个）
        if (sessionContext.size() > 100) {
            sessionContext.clear();
        }

        return ChatResponse.builder()
                .reply(reply)
                .sessionId(sessionId)
                .intent(intent)
                .build();
    }

    private String recognizeIntent(String message, Map<String, String> context) {
        String lower = message.toLowerCase();

        // 查看预约
        if (containsAny(lower, "我的预约", "预约情况", "查看预约", "预约列表", "当前预约")) {
            return "QUERY_RESERVATIONS";
        }

        // 查看自习室/座位
        if (containsAny(lower, "有哪些自习室", "自习室列表", "查看自习室", "什么自习室")) {
            return "QUERY_ROOMS";
        }
        if (containsAny(lower, "有空座", "空座位", "可用座位", "找座位", "搜索座位", "查座位", "空座", "座位")) {
            return "QUERY_AVAILABLE_SEATS";
        }

        // 预约
        if (containsAny(lower, "预约", "预定", "订座位", "订一个")) {
            return "MAKE_RESERVATION";
        }

        // 取消预约
        if (containsAny(lower, "取消预约", "取消", "退订")) {
            return "CANCEL_RESERVATION";
        }

        // 签到
        if (containsAny(lower, "签到", "如何签到", "怎么签到")) {
            return "CHECK_IN_HELP";
        }

        // 违约
        if (containsAny(lower, "违约", "违约记录")) {
            return "QUERY_VIOLATIONS";
        }

        // 帮助
        if (containsAny(lower, "帮助", "help", "能做什么", "功能", "使用说明")) {
            return "HELP";
        }

        // 问候
        if (containsAny(lower, "你好", "hi", "hello", "嗨")) {
            return "GREETING";
        }

        return "UNKNOWN";
    }

    private String handleIntent(String intent, String message, Long userId, Map<String, String> context) {
        User user = userMapper.selectById(userId);

        switch (intent) {
            case "GREETING":
                return String.format("你好%s！我是 SeatFlow 智能助手 🤖\n\n我可以帮你：\n• 查看自习室和空座位\n• 预约/取消预约\n• 查看你的预约和违约记录\n• 签到指引\n\n请问有什么可以帮你的？",
                        user != null && user.getRealName() != null ? "，" + user.getRealName() : "");

            case "HELP":
                return "🤖 SeatFlow 智能助手使用指南：\n\n" +
                        "📍 查看自习室：\"有哪些自习室？\"\n" +
                        "🔍 查找空座：\"有空座位吗？\"\n" +
                        "📋 我的预约：\"查看我的预约\"\n" +
                        "📝 预约座位：\"预约图书馆301的A1座位 明天 8:00-10:00\"\n" +
                        "❌ 取消预约：\"取消预约\"\n" +
                        "✅ 签到指引：\"怎么签到？\"\n" +
                        "⚠️ 违约记录：\"查看违约记录\"";

            case "QUERY_ROOMS":
                return handleQueryRooms();

            case "QUERY_AVAILABLE_SEATS":
                return handleQueryAvailableSeats(message);

            case "QUERY_RESERVATIONS":
                return handleQueryReservations(userId);

            case "MAKE_RESERVATION":
                return handleMakeReservation(message, userId, context);

            case "CANCEL_RESERVATION":
                return handleCancelReservation(userId);

            case "CHECK_IN_HELP":
                return "✅ 签到步骤：\n\n" +
                        "1. 前往自习室，获取今日签到编码（由管理员出示）\n" +
                        "2. 在「签到」页面输入6位编码\n" +
                        "3. 选择要签到的预约，点击签到\n\n" +
                        "⚠️ 注意：需要在预约开始前15分钟到结束时间内签到，超时15分钟将自动取消并记录违约。";

            case "QUERY_VIOLATIONS":
                return "你可以通过「违约记录」页面查看你的违约情况。超时未签到将自动记录违约，多次违约可能影响预约权限。";

            case "AUTH_REQUIRED":
                return "请先登录后再使用智能助手。";

            default:
                return "抱歉，我没有理解你的意思 🤔\n\n你可以试试：\n• \"有哪些自习室？\"\n• \"有空座位吗？\"\n• \"查看我的预约\"\n• \"帮助\"";
        }
    }

    private String handleQueryRooms() {
        List<Room> rooms = roomMapper.selectList(
                new LambdaQueryWrapper<Room>().eq(Room::getStatus, "OPEN").orderByAsc(Room::getId));

        if (rooms.isEmpty()) return "目前没有开放的自习室 😢";

        StringBuilder sb = new StringBuilder("📚 当前开放的自习室：\n\n");
        for (Room room : rooms) {
            long total = seatMapper.selectCount(new LambdaQueryWrapper<Seat>().eq(Seat::getRoomId, room.getId()));
            long available = seatMapper.selectCount(
                    new LambdaQueryWrapper<Seat>().eq(Seat::getRoomId, room.getId()).eq(Seat::getStatus, "AVAILABLE"));
            sb.append(String.format("• %s（%s）\n  时间: %s-%s | 空座: %d/%d\n\n",
                    room.getName(), room.getLocation() != null ? room.getLocation() : "",
                    room.getOpenTime().toString().substring(0, 5),
                    room.getCloseTime().toString().substring(0, 5),
                    available, total));
        }
        sb.append("想查看某个自习室的详细座位图，可以说\"有空座位吗？\"");
        return sb.toString();
    }

    private String handleQueryAvailableSeats(String message) {
        String lower = message.toLowerCase();

        // 解析位置偏好
        String positionFilter = null;
        if (containsAny(lower, "靠窗", "窗户", "窗边")) positionFilter = "WINDOW";
        else if (containsAny(lower, "靠走廊", "走廊", "过道")) positionFilter = "CORRIDOR";

        // 解析插座偏好
        String socketFilter = null;
        if (containsAny(lower, "插座", "插电", "充电", "固定插")) socketFilter = "FIXED";
        else if (containsAny(lower, "移动导轨", "导轨")) socketFilter = "TRACK";

        List<Room> rooms = roomMapper.selectList(
                new LambdaQueryWrapper<Room>().eq(Room::getStatus, "OPEN").orderByAsc(Room::getId));

        if (rooms.isEmpty()) return "目前没有开放的自习室 😢";

        StringBuilder sb = new StringBuilder("🔍 今日可用座位：\n\n");
        boolean hasAvailable = false;
        for (Room room : rooms) {
            LambdaQueryWrapper<Seat> seatQuery = new LambdaQueryWrapper<Seat>()
                    .eq(Seat::getRoomId, room.getId())
                    .eq(Seat::getStatus, "AVAILABLE");
            if (positionFilter != null) seatQuery.eq(Seat::getPosition, positionFilter);
            if (socketFilter != null) seatQuery.eq(Seat::getSocketType, socketFilter);

            List<Seat> seats = seatMapper.selectList(seatQuery);
            if (!seats.isEmpty()) {
                hasAvailable = true;
                sb.append(String.format("📍 %s：\n", room.getName()));
                // 按插座类型分组
                Map<String, List<Seat>> bySocket = new LinkedHashMap<>();
                for (Seat s : seats) {
                    bySocket.computeIfAbsent(s.getSocketType(), k -> new ArrayList<>()).add(s);
                }
                bySocket.forEach((type, list) -> {
                    String typeLabel = "FIXED".equals(type) ? "⚡固定插座" : "MOVABLE".equals(type) || "TRACK".equals(type) ? "🔌移动导轨" : "普通";
                    sb.append(String.format("  %s: %s\n", typeLabel,
                            list.stream().map(Seat::getSeatNumber).limit(10).reduce((a, b) -> a + ", " + b).orElse("")));
                    if (list.size() > 10) sb.append(" 等").append(list.size()).append("个");
                });
                sb.append("\n");
            }
        }

        if (!hasAvailable) {
            if (positionFilter != null || socketFilter != null) {
                return "没有找到符合条件" + (positionFilter != null ? "(" + (positionFilter.equals("WINDOW") ? "靠窗" : "靠走廊") + ")" : "") + "的可用座位 😢\n\n试试说\"有空座吗？\"查看所有可用座位";
            }
            return "今天没有可用座位 😢";
        }
        sb.append("\n想要预约，可以说\"预约 [自习室名] [座位号] [日期] [时段]\"");
        return sb.toString();
    }

    private String handleQueryReservations(Long userId) {
        List<Reservation> current = reservationMapper.selectList(
                new LambdaQueryWrapper<Reservation>()
                        .eq(Reservation::getUserId, userId)
                        .in(Reservation::getStatus, List.of("PENDING", "CHECKED_IN"))
                        .orderByDesc(Reservation::getDate));

        if (current.isEmpty()) return "你目前没有进行中的预约 📭\n\n想要预约，可以说\"预约 [自习室名] [座位号] [日期] [时段]\"";

        StringBuilder sb = new StringBuilder("📋 你的当前预约：\n\n");
        for (Reservation r : current) {
            Seat seat = seatMapper.selectById(r.getSeatId());
            Room room = seat != null ? roomMapper.selectById(seat.getRoomId()) : null;
            String statusLabel = "PENDING".equals(r.getStatus()) ? "⏳ 待签到" : "✅ 已签到";
            sb.append(String.format("• %s - 座位%s\n  %s %s-%s %s\n\n",
                    room != null ? room.getName() : "未知",
                    seat != null ? seat.getSeatNumber() : "未知",
                    r.getDate(), r.getStartTime().toString().substring(0, 5),
                    r.getEndTime().toString().substring(0, 5), statusLabel));
        }
        return sb.toString();
    }

    private String handleMakeReservation(String message, Long userId, Map<String, String> context) {
        // 尝试解析预约信息
        // 简单模式：预约 [日期] [时段]
        // 支持的日期格式：今天、明天、后天、具体日期(05-18)
        LocalDate date = null;
        String lower = message.toLowerCase();

        if (lower.contains("今天") || lower.contains("今日")) date = LocalDate.now();
        else if (lower.contains("明天") || lower.contains("明日")) date = LocalDate.now().plusDays(1);
        else if (lower.contains("后天")) date = LocalDate.now().plusDays(2);
        else {
            // 尝试解析 MM-dd 或 MM/dd 格式
            Pattern datePattern = Pattern.compile("(\\d{1,2})[-/](\\d{1,2})");
            Matcher m = datePattern.matcher(message);
            if (m.find()) {
                try {
                    int month = Integer.parseInt(m.group(1));
                    int day = Integer.parseInt(m.group(2));
                    date = LocalDate.of(2026, month, day);
                } catch (Exception e) { /* ignore */ }
            }
        }

        // 尝试解析时段 HH:MM-HH:MM 或 HH-HH
        String startTime = null, endTime = null;
        Pattern timePattern = Pattern.compile("(\\d{1,2})(?::(\\d{2}))?\\s*[-~到至]\\s*(\\d{1,2})(?::(\\d{2}))?");
        Matcher tm = timePattern.matcher(message);
        if (tm.find()) {
            startTime = String.format("%02d:%02d", Integer.parseInt(tm.group(1)), tm.group(2) != null ? Integer.parseInt(tm.group(2)) : 0);
            endTime = String.format("%02d:%02d", Integer.parseInt(tm.group(3)), tm.group(4) != null ? Integer.parseInt(tm.group(4)) : 0);
        }

        if (date == null || startTime == null || endTime == null) {
            return "📝 预约需要提供以下信息：\n\n" +
                    "• 日期：今天/明天/后天，或具体日期如 05-18\n" +
                    "• 时段：如 8:00-10:00 或 8-10\n\n" +
                    "示例：\"预约 明天 8:00-10:00\"\n\n" +
                    "💡 预约后我会帮你找第一个可用座位！";
        }

        // 找一个可用座位
        List<Room> rooms = roomMapper.selectList(new LambdaQueryWrapper<Room>().eq(Room::getStatus, "OPEN"));
        for (Room room : rooms) {
            List<Seat> seats = seatMapper.selectList(
                    new LambdaQueryWrapper<Seat>().eq(Seat::getRoomId, room.getId()).eq(Seat::getStatus, "AVAILABLE"));
            for (Seat seat : seats) {
                // 检查该座位在该时段是否可用
                LambdaQueryWrapper<Reservation> conflict = new LambdaQueryWrapper<>();
                conflict.eq(Reservation::getSeatId, seat.getId())
                        .eq(Reservation::getDate, date)
                        .in(Reservation::getStatus, List.of("PENDING", "CHECKED_IN"))
                        .lt(Reservation::getStartTime, java.time.LocalTime.parse(endTime))
                        .gt(Reservation::getEndTime, java.time.LocalTime.parse(startTime));
                if (reservationMapper.selectCount(conflict) == 0) {
                    // 找到可用座位，尝试创建预约
                    try {
                        com.seatflow.dto.request.ReservationCreateRequest req = new com.seatflow.dto.request.ReservationCreateRequest();
                        req.setSeatId(seat.getId());
                        req.setDate(date.toString());
                        req.setStartTime(startTime);
                        req.setEndTime(endTime);
                        var result = reservationService.create(req);
                        return String.format("✅ 预约成功！\n\n📍 %s - 座位 %s\n📅 %s\n🕐 %s-%s\n🆔 预约ID: %d\n\n请按时签到哦！",
                                room.getName(), seat.getSeatNumber(), date, startTime, endTime, result.getId());
                    } catch (Exception e) {
                        return "❌ 预约失败：" + e.getMessage() + "\n\n请重试或手动在预约页面操作。";
                    }
                }
            }
        }

        return "❌ 抱歉，" + date + " " + startTime + "-" + endTime + " 没有找到可用座位 😢\n\n你可以试试其他时段，或在座位页面手动选择。";
    }

    private String handleCancelReservation(Long userId) {
        List<Reservation> pending = reservationMapper.selectList(
                new LambdaQueryWrapper<Reservation>()
                        .eq(Reservation::getUserId, userId)
                        .eq(Reservation::getStatus, "PENDING")
                        .orderByDesc(Reservation::getDate));

        if (pending.isEmpty()) return "你目前没有待签到的预约可以取消 📭";

        if (pending.size() == 1) {
            try {
                reservationService.cancel(pending.get(0).getId());
                return "✅ 已成功取消预约！";
            } catch (Exception e) {
                return "❌ 取消失败：" + e.getMessage();
            }
        }

        StringBuilder sb = new StringBuilder("你有多个待签到预约，请在「我的预约」页面手动选择取消：\n\n");
        for (Reservation r : pending) {
            Seat seat = seatMapper.selectById(r.getSeatId());
            Room room = seat != null ? roomMapper.selectById(seat.getRoomId()) : null;
            sb.append(String.format("• ID:%d - %s 座位%s %s %s-%s\n",
                    r.getId(), room != null ? room.getName() : "", seat != null ? seat.getSeatNumber() : "",
                    r.getDate(), r.getStartTime().toString().substring(0, 5), r.getEndTime().toString().substring(0, 5)));
        }
        return sb.toString();
    }

    private boolean containsAny(String text, String... keywords) {
        for (String kw : keywords) {
            if (text.contains(kw)) return true;
        }
        return false;
    }
}
