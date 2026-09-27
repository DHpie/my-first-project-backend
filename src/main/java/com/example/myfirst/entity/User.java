package com.example.myfirst.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@Entity
@Table(name = "user")
public class User extends BaseEntity {

    @Column(nullable = false, length = 50, unique = true)
    private String username;

    @Column(nullable = false, length = 100)
    private String email;

    @Column(length = 30)
    private String nickname;

    @Column(length = 200)
    private String bio;

    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;

    // 以 JSON 数组序列化存储，如 ["Food","History"]
    @Column(name = "interest_tags", length = 200)
    private String interestTags;
}
