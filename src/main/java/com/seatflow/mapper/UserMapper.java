package com.seatflow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.seatflow.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {
}
