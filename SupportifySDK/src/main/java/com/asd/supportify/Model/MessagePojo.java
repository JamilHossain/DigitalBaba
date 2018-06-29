package com.asd.supportify.Model;

import com.google.firebase.database.Exclude;

import java.util.Date;

/**
 * Created by bliveinhack on 28/9/17.
 */

public class MessagePojo  {

   /*...*/
   String id;
    String text;

    public String getImage() {
        return image!=null?image.trim():null;
    }

    public void setImage(String image) {
        this.image = image;
    }

    String image;

    public void setId(String id) {
        this.id = id;
    }

    public void setText(String text) {
        this.text = text;
    }

    public void setCreatedAt(Long createdAt) {
        this.createdAt = createdAt;
    }

    public void setUser(AuthorPojo user) {
        this.user = user;
    }

    Long createdAt;
    AuthorPojo user;


    public String getId() {
        return id;
    }


    public String getText() {
        return text;
    }


    public AuthorPojo getUser() {
        return user;
    }


    public Long getCreatedAt() {
        return createdAt;
    }

    @Exclude
    public Message getM() {
        Message m=new Message();
        m.setText(text);
        m.setId(id);
        m.setCreatedAt(new Date(createdAt));
        Author a=new Author();
        a.setName(getUser().getName());
        a.setId(getUser().getId());
        a.setAvatar(getUser().getAvatar());
        m.setUser(a);
        m.setImage(getImage());
        return m;
    }
}
