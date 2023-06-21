package com.ssafy.ssafsound.domain.member.service;

import com.ssafy.ssafsound.domain.auth.dto.AuthenticatedMember;
import com.ssafy.ssafsound.domain.member.domain.Member;
import com.ssafy.ssafsound.domain.member.domain.MemberRole;
import com.ssafy.ssafsound.domain.member.domain.MemberToken;
import com.ssafy.ssafsound.domain.member.domain.OAuthType;
import com.ssafy.ssafsound.domain.member.dto.PostMemberReqDto;
import com.ssafy.ssafsound.domain.member.exception.MemberErrorInfo;
import com.ssafy.ssafsound.domain.member.exception.MemberException;
import com.ssafy.ssafsound.domain.member.repository.MemberRepository;
import com.ssafy.ssafsound.domain.member.repository.MemberRoleRepository;
import com.ssafy.ssafsound.domain.member.repository.MemberTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;
    private final MemberRoleRepository memberRoleRepository;
    private final MemberTokenRepository memberTokenRepository;

    @Transactional
     public AuthenticatedMember createMemberByOauthIdentifier(PostMemberReqDto postMemberReqDto) {
        Optional<Member> optionalMember = memberRepository.findByOauthIdentifier(postMemberReqDto.getOauthIdentifier());
        Member member;
        if (optionalMember.isPresent()) {
             member = optionalMember.get();
             if(isInvalidOauthLogin(member, postMemberReqDto)) throw new MemberException(MemberErrorInfo.MEMBER_OAUTH_NOT_FOUND);
        } else {
            MemberRole memberRole = findMemberRoleByRoleName("user");
            member = postMemberReqDto.createMember();
            member.setMemberRole(memberRole);
            memberRepository.save(member);
        }
        return AuthenticatedMember.of(member);
     }

    @Transactional
    public void saveTokenByMember(AuthenticatedMember authenticatedMember, String accessToken, String refreshToken) {
        Optional<MemberToken> memberTokenOptional = memberTokenRepository.findById(authenticatedMember.getMemberId());
        MemberToken memberToken;

        if (memberTokenOptional.isPresent()) {
            memberToken = memberTokenOptional.get();
            memberToken.changeAccessTokenByLogin(accessToken);
            memberToken.changeRefreshTokenByLogin(refreshToken);
        } else {
            Member member = memberRepository.findById(authenticatedMember.getMemberId()).orElseThrow(() -> new MemberException(MemberErrorInfo.MEMBER_NOT_FOUND_BY_ID));
            memberToken = MemberToken.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .member(member)
                    .build();
        }
        memberTokenRepository.save(memberToken);
    }

    public MemberRole findMemberRoleByRoleName(String roleType) {
        return memberRoleRepository.findByRoleType(roleType).orElseThrow(() -> new MemberException(MemberErrorInfo.MEMBER_ROLE_TYPE_NOT_FOUND));
    }

    public boolean isInvalidOauthLogin(Member member, PostMemberReqDto postMemberReqDto) {
        OAuthType oAuthType = member.getOauthType();
        return !member.getOauthIdentifier().equals(postMemberReqDto.getOauthIdentifier()) || !oAuthType.isEqual(postMemberReqDto.getOauthName());
    }
}
