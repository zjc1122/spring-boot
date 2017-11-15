package cn.zjc.server.user.user.impl;

import cn.zjc.mapper.UserMapper;
import cn.zjc.model.User;
import cn.zjc.server.user.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Created by zhangjiacheng on 2017/11/15.
 */
@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserMapper userMapper;

    @Override
    public int addUser(User user) {
        return 0;
    }

    @Override
    public List<User> findAllUser(int pageNum, int pageSize) {

       // PageHelper.startPage(pageNum, pageSize);
        return userMapper.selectAllUser();
    }
}