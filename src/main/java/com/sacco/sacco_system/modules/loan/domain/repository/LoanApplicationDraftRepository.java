package com.sacco.sacco_system.modules.loan.domain.repository;

import com.sacco.sacco_system.modules.loan.domain.entity.LoanApplicationDraft;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LoanApplicationDraftRepository extends JpaRepository<LoanApplicationDraft, UUID> {

    // Find active drafts to resume (e.g. Pending Fee or Paid but not Converted)
    Optional<LoanApplicationDraft> findFirstByMemberIdAndStatusIn(
            UUID memberId,
            List<LoanApplicationDraft.DraftStatus> statuses
    );
}