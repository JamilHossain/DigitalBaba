package com.asd.supportify.Model;

public class AuthorPojo {

   /*...*/
   String id;
    String name;
    String avatar;

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    String token;


   public String getId() {
       return id;
   }


   public String getName() {
       return name;
   }


   public String getAvatar() {
       return avatar;
   }

    public void setId(String id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }
}