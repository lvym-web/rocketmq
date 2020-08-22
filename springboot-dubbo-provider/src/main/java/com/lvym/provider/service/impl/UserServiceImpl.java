package com.lvym.provider.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.lvym.server.IUserService;
@Service
public class UserServiceImpl implements IUserService {

    @Override
    public String sayHello(String name) {
        return "hello"+name;
    }
}
