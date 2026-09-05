package github.comioko.articlepilot.service;

import github.comioko.articlepilot.exception.BusinessException;
import github.comioko.articlepilot.mapper.ArticleMapper;
import github.comioko.articlepilot.mapper.ArticleRevisionMapper;
import github.comioko.articlepilot.model.dto.article.ArticleSaveRevisionRequest;
import github.comioko.articlepilot.model.dto.article.ArticleRestoreRevisionRequest;
import github.comioko.articlepilot.model.entity.Article;
import github.comioko.articlepilot.model.entity.ArticleRevision;
import github.comioko.articlepilot.model.entity.User;
import github.comioko.articlepilot.service.impl.ArticleServiceImpl;
import github.comioko.articlepilot.utils.ArticleFingerprint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Service regression checks without remote models or a live database. */
class ArticleWorkbenchTest {
    private ArticleServiceImpl service;
    private ArticleMapper articles;
    private ArticleRevisionMapper revisions;
    private Article article;
    private User owner;

    @BeforeEach void setup() {
        service = spy(new ArticleServiceImpl());
        articles = mock(ArticleMapper.class); revisions = mock(ArticleRevisionMapper.class);
        doReturn(articles).when(service).getMapper();
        ReflectionTestUtils.setField(service, "articleRevisionMapper", revisions);
        article = Article.builder().id(1L).taskId("task").userId(8L).status("COMPLETED")
                .content("原文").fullContent("原文\n\n![图片](https://example.com/a.png)").build();
        owner = User.builder().id(8L).userRole("user").build();
        when(articles.selectForEditing("task")).thenReturn(article);
    }

    private ArticleSaveRevisionRequest request() {
        var request = new ArticleSaveRevisionRequest();
        request.setTaskId("task"); request.setBaseFingerprint(ArticleFingerprint.of(article));
        request.setContent("精修稿"); request.setFullContent("精修稿");
        return request;
    }

    @Test void conflictNeverOverwritesOrCreatesHistory() {
        var request = request(); request.setBaseFingerprint("stale");
        assertThrows(BusinessException.class, () -> service.saveRevision(request, owner));
        verifyNoInteractions(revisions);
        assertEquals("原文", article.getContent());
    }

    @Test void otherUsersCannotSaveOrRestore() {
        var stranger = User.builder().id(9L).userRole("user").build();
        assertThrows(BusinessException.class, () -> service.saveRevision(request(), stranger));
        var restore = new ArticleRestoreRevisionRequest(); restore.setTaskId("task"); restore.setRevisionId(1L);
        assertThrows(BusinessException.class, () -> service.restoreRevision(restore, stranger));
        verifyNoInteractions(revisions);
    }

    @Test void generationInProgressCannotBeOverwritten() {
        article.setStatus("PROCESSING");
        assertThrows(BusinessException.class, () -> service.saveRevision(request(), owner));
        verifyNoInteractions(revisions);
    }

    @Test void firstSavePreservesOriginalAndCreatesNewSnapshot() {
        var first = ArticleRevision.builder().revisionNumber(1).build();
        when(revisions.selectListByQuery(any())).thenReturn(List.of(), List.of(), List.of(first));
        doReturn(true).when(service).updateById(any(Article.class));
        service.saveRevision(request(), owner);
        var snapshots = ArgumentCaptor.forClass(ArticleRevision.class);
        verify(revisions, times(2)).insert(snapshots.capture());
        assertEquals("原文", snapshots.getAllValues().get(0).getContent());
        assertTrue(snapshots.getAllValues().get(0).getFullContent().contains("![图片]"));
        assertEquals("精修稿", snapshots.getAllValues().get(1).getFullContent());
        assertEquals(2, snapshots.getAllValues().get(1).getRevisionNumber());
    }

    @Test void restorePreservesCurrentTextAndRecordsRestoredVersion() {
        var previous = ArticleRevision.builder().id(33L).revisionNumber(1).content("旧稿").fullContent("旧图文").build();
        var backup = ArticleRevision.builder().revisionNumber(2).build();
        when(revisions.selectListByQuery(any())).thenReturn(List.of(previous), List.of(previous), List.of(previous, backup));
        doReturn(true).when(service).updateById(any(Article.class));
        var request = new ArticleRestoreRevisionRequest(); request.setTaskId("task"); request.setRevisionId(33L);
        request.setBaseFingerprint(ArticleFingerprint.of(article));
        var result = service.restoreRevision(request, owner);
        var snapshots = ArgumentCaptor.forClass(ArticleRevision.class);
        verify(revisions, times(2)).insert(snapshots.capture());
        assertEquals("原文", snapshots.getAllValues().get(0).getContent());
        assertEquals("旧图文", snapshots.getAllValues().get(1).getFullContent());
        assertEquals(3, snapshots.getAllValues().get(1).getRevisionNumber());
        assertEquals("旧图文", result.getFullContent());
        assertEquals(ArticleFingerprint.of(article), result.getContentFingerprint());
    }

    @Test void revisionMustBelongToThisArticle() {
        when(revisions.selectListByQuery(any())).thenReturn(List.of());
        var request = new ArticleRestoreRevisionRequest(); request.setTaskId("task"); request.setRevisionId(100L);
        assertThrows(BusinessException.class, () -> service.restoreRevision(request, owner));
        verify(revisions, never()).insert(any(ArticleRevision.class));
    }
}
