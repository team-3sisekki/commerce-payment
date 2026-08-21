package org.example.commercepayment.domain.member.service;

import lombok.RequiredArgsConstructor;
import org.example.commercepayment.domain.member.dto.GetMeResponse;
import org.example.commercepayment.domain.member.entity.Member;
import org.example.commercepayment.domain.member.repository.MemberRepository;
import org.example.commercepayment.global.error.BusinessException;
import org.example.commercepayment.global.error.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;

    // 내 정보 보기
    @Transactional(readOnly = true)
    public GetMeResponse getOne (Long id) {
        // findById메서드를 만들었기 때문에 코드를 깔끔하게 수정
        Member member = findById(id);
        return GetMeResponse.from(member);
    }

    // CartFacade 혹은 다른 서비스 등 에서 멤버 엔티티가 필요할 때 사용할 메서드
    @Transactional(readOnly = true)
    public Member findById(Long id) {
        return memberRepository.findById(id).orElseThrow(
                () -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND)
        );
    }

}
