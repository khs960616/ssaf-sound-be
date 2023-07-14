package com.ssafy.ssafsound.domain.comment.service;

import com.ssafy.ssafsound.domain.auth.dto.AuthenticatedMember;
import com.ssafy.ssafsound.domain.comment.domain.Comment;
import com.ssafy.ssafsound.domain.comment.domain.CommentNumber;
import com.ssafy.ssafsound.domain.comment.dto.GetCommentResDto;
import com.ssafy.ssafsound.domain.comment.dto.PostCommentWriteReplyReqDto;
import com.ssafy.ssafsound.domain.comment.dto.PostCommentWriteReqDto;
import com.ssafy.ssafsound.domain.comment.dto.PutCommentUpdateReqDto;
import com.ssafy.ssafsound.domain.comment.exception.CommentErrorInfo;
import com.ssafy.ssafsound.domain.comment.exception.CommentException;
import com.ssafy.ssafsound.domain.comment.repository.CommentNumberRepository;
import com.ssafy.ssafsound.domain.comment.repository.CommentRepository;
import com.ssafy.ssafsound.domain.member.domain.Member;
import com.ssafy.ssafsound.domain.member.exception.MemberErrorInfo;
import com.ssafy.ssafsound.domain.member.exception.MemberException;
import com.ssafy.ssafsound.domain.member.repository.MemberRepository;
import com.ssafy.ssafsound.domain.post.exception.PostErrorInfo;
import com.ssafy.ssafsound.domain.post.exception.PostException;
import com.ssafy.ssafsound.domain.post.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class CommentService {
    private final CommentRepository commentRepository;
    private final CommentNumberRepository commentNumberRepository;
    private final PostRepository postRepository;
    private final MemberRepository memberRepository;


    @Transactional
    public Long writeComment(Long postId, Long memberId, PostCommentWriteReqDto postCommentWriteReqDto) {
        if (!postRepository.existsById(postId)) {
            throw new PostException(PostErrorInfo.NOT_FOUND_POST);
        }

        // 1. 익명 번호 부여
        CommentNumber commentNumber = commentNumberRepository.
                findByPostIdAndMemberId(postId, memberId).orElse(null);

        if (commentNumber == null) {
            commentNumber = CommentNumber.builder()
                    .post(postRepository.getReferenceById(postId))
                    .member(memberRepository.getReferenceById(memberId))
                    .number(commentNumberRepository.countAllByPostId(postId) + 1)
                    .build();
            commentNumberRepository.save(commentNumber);
        }

        // 2. 댓글 저장
        Comment comment = Comment.builder()
                .post(postRepository.getReferenceById(postId))
                .member(memberRepository.getReferenceById(memberId))
                .content(postCommentWriteReqDto.getContent())
                .anonymous(postCommentWriteReqDto.getAnonymous())
                .commentNumber(commentNumber)
                .commentGroup(null)
                .build();

        comment = commentRepository.save(comment);
        comment.setCommentGroup(comment);

        return comment.getId();
    }

    @Transactional(readOnly = true)
    public GetCommentResDto findComments(Long postId, AuthenticatedMember member, Pageable pageable) {
          if (!postRepository.existsById(postId)) {
            throw new PostException(PostErrorInfo.NOT_FOUND_POST);
        }
  
        PageRequest pageRequest = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
        List<Comment> comments = commentRepository.findAllPostIdWithDetailsFetchOrderByCommentGroupId(postId, pageRequest);
        return GetCommentResDto.of(comments, member);
    }
    @Transactional
    public Long updateComment(Long commentId, Long memberId, PutCommentUpdateReqDto putCommentUpdateReqDto) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new CommentException(CommentErrorInfo.NOT_FOUND_COMMENT));

        if (!comment.getMember().getId().equals(memberId)) {
            throw new CommentException(CommentErrorInfo.UNAUTHORIZED_UPDATE_COMMENT);
        }

        comment.updateComment(putCommentUpdateReqDto.getContent(), putCommentUpdateReqDto.getAnonymous());
        return comment.getId();
    }

    @Transactional
    public Long writeCommentReply(Long postId, Long commentId, Long memberId, PostCommentWriteReplyReqDto postCommentWriteReplyReqDto) {
        if (!postRepository.existsById(postId)) {
            throw new PostException(PostErrorInfo.NOT_FOUND_POST);
        }
      
        if (!commentRepository.existsById(commentId)) {
            throw new CommentException(CommentErrorInfo.NOT_FOUND_COMMENT);
        }

        // 1. 익명 번호 부여
        CommentNumber commentNumber = commentNumberRepository.
                findByPostIdAndMemberId(postId, memberId).orElse(null);

        if (commentNumber == null) {
            commentNumber = CommentNumber.builder()
                    .post(postRepository.getReferenceById(postId))
                    .member(memberRepository.getReferenceById(memberId))
                    .number(commentNumberRepository.countAllByPostId(postId) + 1)
                    .build();
            commentNumberRepository.save(commentNumber);
        }

        // 2. 대댓글 저장
        Comment comment = Comment.builder()
                .post(postRepository.getReferenceById(postId))
                .member(memberRepository.getReferenceById(memberId))
                .content(postCommentWriteReplyReqDto.getContent())
                .anonymous(postCommentWriteReplyReqDto.getAnonymous())
                .commentNumber(commentNumber)
                .commentGroup(commentRepository.getReferenceById(commentId))
                .build();

        comment = commentRepository.save(comment);
        return comment.getId();
    }

    @Transactional
    public Long deleteComment(Long commentId, Long memberId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new CommentException(CommentErrorInfo.NOT_FOUND_COMMENT));

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new MemberException(MemberErrorInfo.MEMBER_NOT_FOUND_BY_ID));

        if (!comment.getMember().getId().equals(member.getId())) {
            throw new CommentException(CommentErrorInfo.UNAUTHORIZED_DELETE_COMMENT);
        }

        commentRepository.delete(comment);
        return comment.getId();
    }
}