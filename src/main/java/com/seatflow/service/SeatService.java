package com.seatflow.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.seatflow.common.exception.BusinessException;
import com.seatflow.dto.request.SeatCreateRequest;
import com.seatflow.dto.request.SeatUpdateRequest;
import com.seatflow.dto.response.SeatResponse;
import com.seatflow.entity.Room;
import com.seatflow.entity.Seat;
import com.seatflow.mapper.RoomMapper;
import com.seatflow.mapper.SeatMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SeatService {

    private final SeatMapper seatMapper;
    private final RoomMapper roomMapper;

    /**
     * 按自习室查询座位列表
     */
    public List<SeatResponse> listByRoom(Long roomId) {
        Room room = roomMapper.selectById(roomId);
        if (room == null) throw new BusinessException(404, "自习室不存在");

        List<Seat> seats = seatMapper.selectList(
                new LambdaQueryWrapper<Seat>()
                        .eq(Seat::getRoomId, roomId)
                        .orderByAsc(Seat::getRowNum)
                        .orderByAsc(Seat::getColNum));

        return seats.stream().map(this::toSeatResponse).collect(Collectors.toList());
    }

    /**
     * 座位详情
     */
    public SeatResponse getById(Long seatId) {
        Seat seat = seatMapper.selectById(seatId);
        if (seat == null) throw new BusinessException(404, "座位不存在");
        return toSeatResponse(seat);
    }

    /**
     * 创建单个座位
     */
    @Transactional
    public SeatResponse create(Long roomId, SeatCreateRequest request) {
        Room room = roomMapper.selectById(roomId);
        if (room == null) throw new BusinessException(404, "自习室不存在");

        // 检查座位编号唯一性
        LambdaQueryWrapper<Seat> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Seat::getRoomId, roomId).eq(Seat::getSeatNumber, request.getSeatNumber());
        if (seatMapper.selectCount(wrapper) > 0) {
            throw new BusinessException(400, "座位编号 " + request.getSeatNumber() + " 已存在");
        }

        Seat seat = new Seat();
        seat.setRoomId(roomId);
        seat.setSeatNumber(request.getSeatNumber());
        seat.setRowNum(request.getRowNum());
        seat.setColNum(request.getColNum());
        seat.setSocketType(request.getSocketType() != null ? request.getSocketType() : "NONE");
        seat.setPosition(request.getPosition() != null ? request.getPosition() : "MIDDLE");
        seat.setStatus("AVAILABLE");
        seatMapper.insert(seat);

        return toSeatResponse(seat);
    }

    /**
     * 批量创建座位
     */
    @Transactional
    public List<SeatResponse> batchCreate(Long roomId, List<SeatCreateRequest> requests) {
        Room room = roomMapper.selectById(roomId);
        if (room == null) throw new BusinessException(404, "自习室不存在");

        return requests.stream().map(req -> create(roomId, req)).collect(Collectors.toList());
    }

    /**
     * 更新座位
     */
    @Transactional
    public SeatResponse update(Long seatId, SeatUpdateRequest request) {
        Seat seat = seatMapper.selectById(seatId);
        if (seat == null) throw new BusinessException(404, "座位不存在");

        if (request.getSeatNumber() != null) seat.setSeatNumber(request.getSeatNumber());
        if (request.getRowNum() != null) seat.setRowNum(request.getRowNum());
        if (request.getColNum() != null) seat.setColNum(request.getColNum());
        if (request.getSocketType() != null) seat.setSocketType(request.getSocketType());
        if (request.getPosition() != null) seat.setPosition(request.getPosition());
        if (request.getStatus() != null) seat.setStatus(request.getStatus());
        seatMapper.updateById(seat);

        return toSeatResponse(seat);
    }

    /**
     * 注销座位（逻辑删除）
     */
    @Transactional
    public void remove(Long seatId) {
        Seat seat = seatMapper.selectById(seatId);
        if (seat == null) throw new BusinessException(404, "座位不存在");
        seatMapper.deleteById(seatId);
    }

    private SeatResponse toSeatResponse(Seat seat) {
        return SeatResponse.builder()
                .id(seat.getId())
                .roomId(seat.getRoomId())
                .seatNumber(seat.getSeatNumber())
                .rowNum(seat.getRowNum())
                .colNum(seat.getColNum())
                .socketType(seat.getSocketType())
                .position(seat.getPosition())
                .status(seat.getStatus())
                .isAvailable("AVAILABLE".equals(seat.getStatus()))
                .build();
    }
}
