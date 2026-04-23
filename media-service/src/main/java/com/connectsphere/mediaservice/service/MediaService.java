package com.connectsphere.mediaservice.service;

import com.connectsphere.mediaservice.entity.Media;

import java.util.List;

public interface MediaService {

    Media uploadMedia(Media media);

    List<Media> getMediaByPost(Long postId);

    void deleteMedia(Long mediaId);
}