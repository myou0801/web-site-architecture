package com.myou.ec.ecsite.infrastructure.repository.user;

import com.myou.ec.ecsite.domain.model.user.MailAddress;
import com.myou.ec.ecsite.domain.model.user.User;
import com.myou.ec.ecsite.domain.model.user.UserName;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Setter
@Getter
@Table(name = "USER")
public class UserEntity {

    @Id
    private Long id;
    private String username;
    private String email;

    public static UserEntity from(User user) {
        UserEntity u = new UserEntity();
        u.setId(user.id());
        u.setUsername(user.userName().userName());
        u.setEmail(user.mailAddress().mailAddress());
        return u;
    }

    public User toUser() {
        return new User(this.getId(),
                new UserName(this.username),
                new MailAddress(this.email));
    }

}
