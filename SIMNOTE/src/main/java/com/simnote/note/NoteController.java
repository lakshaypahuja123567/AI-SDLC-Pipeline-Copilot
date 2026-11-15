package com.simnote.note;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;
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
import org.springframework.web.server.ResponseStatusException;
import com.simnote.auth.AppUser;
import com.simnote.auth.AppUserRepository;

@RestController
@RequestMapping("/api/notes")
public class NoteController {

    public record NoteRequest(@NotBlank @Size(max = 255) String title, @Size(max = 10000) String content) {
    }

    public record NoteResponse(Long id, String title, String content, Instant createdAt, Instant updatedAt) {
        static NoteResponse from(Note note) {
            return new NoteResponse(note.getId(), note.getTitle(), note.getContent(),
                    note.getCreatedAt(), note.getUpdatedAt());
        }
    }

    private final NoteRepository notes;
    private final AppUserRepository users;

    public NoteController(NoteRepository notes, AppUserRepository users) {
        this.notes = notes;
        this.users = users;
    }

    @GetMapping
    public List<NoteResponse> list(Authentication authentication) {
        return notes.findAllByUserIdOrderByUpdatedAtDescIdDesc(currentUser(authentication).getId()).stream()
                .map(NoteResponse::from)
                .toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public NoteResponse create(@Valid @RequestBody NoteRequest request, Authentication authentication) {
        Note note = new Note(currentUser(authentication), request.title(), request.content(), Instant.now());
        return NoteResponse.from(notes.save(note));
    }

    @GetMapping("/{id}")
    public NoteResponse get(@PathVariable Long id, Authentication authentication) {
        return NoteResponse.from(findOwned(id, authentication));
    }

    @PutMapping("/{id}")
    public NoteResponse update(@PathVariable Long id, @Valid @RequestBody NoteRequest request,
                               Authentication authentication) {
        Note note = findOwned(id, authentication);
        note.update(request.title(), request.content(), Instant.now());
        return NoteResponse.from(notes.save(note));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id, Authentication authentication) {
        notes.delete(findOwned(id, authentication));
    }

    private Note findOwned(Long id, Authentication authentication) {
        return notes.findByIdAndUserId(id, currentUser(authentication).getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Note not found"));
    }

    private AppUser currentUser(Authentication authentication) {
        return users.findByUsername(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    }
}
