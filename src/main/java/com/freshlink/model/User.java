package com.freshlink.model;

import com.freshlink.enums.Role;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(name = "user_type")
@NoArgsConstructor
@Table(name ="user_table")
@Access(AccessType.FIELD) 
public abstract class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    protected Long id;

    @Column(nullable = false)
    protected String name;

    @Email
    @Column(nullable = false, unique = true)
    protected String email;

    @Column(nullable = false)
    protected String phone;

    protected boolean active = true;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    protected Role role;

    protected String passwordHash;
    
    protected Boolean emailVerified;

	 
 
}


