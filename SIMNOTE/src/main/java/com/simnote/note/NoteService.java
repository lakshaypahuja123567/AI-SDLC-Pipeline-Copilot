package com.simnote.note;

import com.simnote.auth.AppUser;
import com.simnote.auth.AppUserRepository;
import com.simnote.dto.NoteRequest;
import com.simnote.dto.NoteResponse;
import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class NoteService {

    private final NoteRepository notes;
    private final AppUserRepository users;

    public NoteService(NoteRepository notes, AppUserRepository users) {
        this.notes = notes;
        this.users = users;
    }

    @Transactional(readOnly = true)
    public List<NoteResponse> list(String username) {
        return notes.findAllByUserIdOrderByUpdatedAtDescIdDesc(currentUser(username).getId()).stream()
                .map(NoteService::toResponse)
                .toList();
    }

    @Transactional
    public NoteResponse create(String username, NoteRequest request) {
        Note note = new Note(currentUser(username), request.title(), request.content(), Instant.now());
        return toResponse(notes.save(note));
    }

    @Transactional(readOnly = true)
    public NoteResponse get(String username, Long id) {
        return toResponse(findOwned(username, id));
    }

    @Transactional
    public NoteResponse update(String username, Long id, NoteRequest request) {
        Note note = findOwned(username, id);
        note.update(request.title(), request.content(), Instant.now());
        return toResponse(notes.save(note));
    }

    @Transactional
    public void delete(String username, Long id) {
        notes.delete(findOwned(username, id));
    }

    private Note findOwned(String username, Long id) {
        return notes.findByIdAndUserId(id, currentUser(username).getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Note not found"));
    }

    private AppUser currentUser(String username) {
        return users.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    }

    private static NoteResponse toResponse(Note note) {
        return new NoteResponse(note.getId(), note.getTitle(), note.getContent(),
                note.getCreatedAt(), note.getUpdatedAt());
    }
}
