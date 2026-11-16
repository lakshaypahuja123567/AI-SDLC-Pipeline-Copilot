package com.simnote.dto;

import jakarta.validation.constraints.Size;

public record NoteRequest(@TrimmedSize(max = 255) String title, @Size(max = 10000) String content) {
}
