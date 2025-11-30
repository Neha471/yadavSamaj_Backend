package com.example.yadavsamaj.service;

import com.example.yadavsamaj.model.TeamMember;
import com.example.yadavsamaj.repository.TeamMemberRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TeamMemberService {

    private final TeamMemberRepository repository;

    // Save or update
    public TeamMember save(TeamMember member) {
        return repository.save(member);
    }

    // Get all
    public List<TeamMember> getAllMembers() {
        return repository.findAll();
    }

    // Get by id
    public Optional<TeamMember> findById(Long id) {
        return repository.findById(id);
    }

    // Delete
    public boolean deleteById(Long id) {
        if (repository.existsById(id)) {
            repository.deleteById(id);
            return true;
        }
        return false;
    }
}
