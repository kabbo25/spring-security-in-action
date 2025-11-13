package com.manning.sbip.ch06.event;

import com.manning.sbip.ch06.entity.ApplicationUser;
import org.springframework.context.ApplicationEvent;

public class UserRegistrationEvent extends ApplicationEvent {

    private final ApplicationUser user;

    public UserRegistrationEvent(Object source, ApplicationUser user) {
        super(source);
        this.user = user;
    }

    public ApplicationUser getUser() {
        return user;
    }
}
