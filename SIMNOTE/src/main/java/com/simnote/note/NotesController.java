package com.simnote.note;

import com.simnote.dto.NoteRequest;
import com.simnote.dto.NoteResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notes")
public class NotesController {

    private final NoteService notes;

    public NotesController(NoteService notes) {
        this.notes = notes;
    }

    @GetMapping
    public List<NoteResponse> list(Authentication authentication) {
        return notes.list(authentication.getName());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public NoteResponse create(@Valid @RequestBody NoteRequest request, Authentication authentication) {
        return notes.create(authentication.getName(), request);
    }

    @GetMapping("/{id}")
    public NoteResponse get(@PathVariable Long id, Authentication authentication) {
        return notes.get(authentication.getName(), id);
    }

    @PutMapping("/{id}")
    public NoteResponse update(@PathVariable Long id, @Valid @RequestBody NoteRequest request,
                               Authentication authentication) {
        return notes.update(authentication.getName(), id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id, Authentication authentication) {
        notes.delete(authentication.getName(), id);
    }
}
