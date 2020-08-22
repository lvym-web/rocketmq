package com.lvym.consumer.controller;

import com.lvym.server.IUserService;
import org.apache.dubbo.config.annotation.Reference;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ConsumerController {

    @Reference
    private IUserService iUserService;

    @GetMapping("/sayHello")
    public String sayHello(String name){

        return iUserService.sayHello("kk");
    }
}
