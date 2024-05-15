package org.example;

import com.xie.rpc.model.User;
import com.xie.rpc.service.UserService;
import com.xie.rpc.springboot.starter.annotation.RpcService;
import org.springframework.stereotype.Service;

/**
 * 用户服务实现类
 */
@Service
@RpcService(serverHost = "localhost", serverPort = 8888)
public class UserServiceImpl implements UserService {

    public User getUser(User user) {
        System.out.println("用户名：" + user.getName());
        return user;
    }
}