package com.seatflow.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.seatflow.common.exception.BusinessException;
import com.seatflow.dto.request.RoomCreateRequest;
import com.seatflow.dto.request.RoomUpdateRequest;
import com.seatflow.dto.response.RoomDetailResponse;
import com.seatflow.dto.response.RoomResponse;
import com.seatflow.dto.response.SeatResponse;
import com.seatflow.entity.Department;
import com.seatflow.entity.Room;
import com.seatflow.entity.Seat;
import com.seatflow.mapper.DepartmentMapper;
import com.seatflow.mapper.RoomMapper;
import com.seatflow.mapper.SeatMapper;
import com.seatflow.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RoomService {

    private final RoomMapper roomMapper;
    private final SeatMapper seatMapper;
    private final DepartmentMapper departmentMapper;

    /**
     * 学生端：查看有权限+开放的自习室列表
     */
    public List<RoomResponse> listForStudent(Long departmentId) {
        LambdaQueryWrapper<Room> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Room::getStatus, "OPEN");
        // 院系权限：自习室 departmentId 为空(全校共享) 或 等于用户的 departmentId
        if (departmentId != null) {
            wrapper.and(w -> w.isNull(Room::getDepartmentId).or().eq(Room::getDepartmentId, departmentId));
        } else {
            wrapper.isNull(Room::getDepartmentId);
        }
        wrapper.orderByAsc(Room::getId);

        List<Room> rooms = roomMapper.selectList(wrapper);
        return rooms.stream().map(this::toRoomResponse).collect(Collectors.toList());
    }

    /**
     * 管理端：全量自习室列表（分页）
     */
    public Page<RoomResponse> listForAdmin(int page, int size, String status, String name) {
        Page<Room> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<Room> wrapper = new LambdaQueryWrapper<>();
        if (status != null) wrapper.eq(Room::getStatus, status);
        if (name != null) wrapper.like(Room::getName, name);
        wrapper.orderByDesc(Room::getId);

        Page<Room> result = roomMapper.selectPage(pageParam, wrapper);
        Page<RoomResponse> responsePage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        responsePage.setRecords(result.getRecords().stream().map(this::toRoomResponse).collect(Collectors.toList()));
        return responsePage;
    }

    /**
     * 自习室详情（含座位概览）
     */
    public RoomDetailResponse getDetail(Long roomId) {
        Room room = roomMapper.selectById(roomId);
        if (room == null) {
            throw new BusinessException(404, "自习室不存在");
        }

        // 检查院系权限
        checkDepartmentAccess(room);

        List<Seat> seats = seatMapper.selectList(
                new LambdaQueryWrapper<Seat>().eq(Seat::getRoomId, roomId).orderByAsc(Seat::getRowNum).orderByAsc(Seat::getColNum));

        List<SeatResponse> seatResponses = seats.stream().map(s -> SeatResponse.builder()
                .id(s.getId())
                .roomId(s.getRoomId())
                .seatNumber(s.getSeatNumber())
                .rowNum(s.getRowNum())
                .colNum(s.getColNum())
                .socketType(s.getSocketType())
                .position(s.getPosition())
                .status(s.getStatus())
                .isAvailable("AVAILABLE".equals(s.getStatus()))
                .build()).collect(Collectors.toList());

        int totalSeats = seats.size();
        int availableSeats = (int) seats.stream().filter(s -> "AVAILABLE".equals(s.getStatus())).count();
        int maxRow = seats.stream().mapToInt(Seat::getRowNum).max().orElse(0);
        int maxCol = seats.stream().mapToInt(Seat::getColNum).max().orElse(0);

        return RoomDetailResponse.builder()
                .id(room.getId())
                .name(room.getName())
                .location(room.getLocation())
                .departmentId(room.getDepartmentId())
                .departmentName(getDepartmentName(room.getDepartmentId()))
                .openTime(room.getOpenTime())
                .closeTime(room.getCloseTime())
                .status(room.getStatus())
                .totalSeats(totalSeats)
                .availableSeats(availableSeats)
                .maxRow(maxRow)
                .maxCol(maxCol)
                .seats(seatResponses)
                .build();
    }

    /**
     * 创建自习室
     */
    @Transactional
    public RoomResponse create(RoomCreateRequest request) {
        Room room = new Room();
        BeanUtils.copyProperties(request, room);
        if (room.getOpenTime() == null) room.setOpenTime(LocalTime.of(7, 0));
        if (room.getCloseTime() == null) room.setCloseTime(LocalTime.of(22, 0));
        room.setStatus("OPEN");
        roomMapper.insert(room);
        return toRoomResponse(room);
    }

    /**
     * 更新自习室
     */
    @Transactional
    public RoomResponse update(Long roomId, RoomUpdateRequest request) {
        Room room = roomMapper.selectById(roomId);
        if (room == null) throw new BusinessException(404, "自习室不存在");
        if (request.getName() != null) room.setName(request.getName());
        if (request.getLocation() != null) room.setLocation(request.getLocation());
        if (request.getDepartmentId() != null) room.setDepartmentId(request.getDepartmentId());
        if (request.getOpenTime() != null) room.setOpenTime(request.getOpenTime());
        if (request.getCloseTime() != null) room.setCloseTime(request.getCloseTime());
        if (request.getStatus() != null) room.setStatus(request.getStatus());
        roomMapper.updateById(room);
        return toRoomResponse(room);
    }

    /**
     * 注销自习室（逻辑删除）
     */
    @Transactional
    public void remove(Long roomId) {
        Room room = roomMapper.selectById(roomId);
        if (room == null) throw new BusinessException(404, "自习室不存在");
        roomMapper.deleteById(roomId);
    }

    private RoomResponse toRoomResponse(Room room) {
        // 计算座位数
        LambdaQueryWrapper<Seat> seatWrapper = new LambdaQueryWrapper<>();
        seatWrapper.eq(Seat::getRoomId, room.getId());
        long total = seatMapper.selectCount(seatWrapper);

        LambdaQueryWrapper<Seat> availWrapper = new LambdaQueryWrapper<>();
        availWrapper.eq(Seat::getRoomId, room.getId()).eq(Seat::getStatus, "AVAILABLE");
        long available = seatMapper.selectCount(availWrapper);

        return RoomResponse.builder()
                .id(room.getId())
                .name(room.getName())
                .location(room.getLocation())
                .departmentId(room.getDepartmentId())
                .departmentName(getDepartmentName(room.getDepartmentId()))
                .openTime(room.getOpenTime())
                .closeTime(room.getCloseTime())
                .status(room.getStatus())
                .totalSeats((int) total)
                .availableSeats((int) available)
                .build();
    }

    private String getDepartmentName(Long departmentId) {
        if (departmentId == null) return "全校共享";
        Department dept = departmentMapper.selectById(departmentId);
        return dept != null ? dept.getName() : "未知";
    }

    private void checkDepartmentAccess(Room room) {
        if (room.getDepartmentId() != null) {
            Long userId = SecurityUtils.getCurrentUserId();
            // If the user is admin, allow access
            if (SecurityUtils.isAdmin()) return;
            // For students, check department
            // This is simplified - full implementation in M5 with RBAC
        }
    }
}
