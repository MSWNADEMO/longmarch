package top.longmarch.cms.service;

import top.longmarch.cms.entity.Article;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 * 文章 服务类
 * </p>
 *
 * @author YuYue
 * @since 2020-01-30
 */
public interface IArticleService extends IService<Article> {

    void saveArticle(Article article);

    void batchPublishArticles();

}
