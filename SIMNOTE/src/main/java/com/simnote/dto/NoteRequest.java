package com.simnote.dto;

public record NoteRequest(@TrimmedSize String title, String content) {
}
