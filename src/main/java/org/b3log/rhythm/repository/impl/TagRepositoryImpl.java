/*
 * Copyright (c) 2010-2017, b3log.org & hacpai.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.b3log.rhythm.repository.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.inject.Inject;
import org.b3log.latke.Keys;
import org.b3log.latke.logging.Level;
import org.b3log.latke.logging.Logger;
import org.b3log.latke.repository.AbstractRepository;
import org.b3log.latke.repository.FilterOperator;
import org.b3log.latke.repository.PropertyFilter;
import org.b3log.latke.repository.Query;
import org.b3log.latke.repository.RepositoryException;
import org.b3log.latke.repository.SortDirection;
import org.b3log.latke.repository.annotation.Repository;
import org.b3log.latke.util.CollectionUtils;
import org.b3log.rhythm.model.Tag;
import org.b3log.rhythm.repository.TagRepository;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Tag repository implementation.
 *
 * @author <a href="http://88250.b3log.org">Liang Ding</a>
 * @version 1.0.0.9, Jun 27, 2012
 * @since 0.1.4
 */
@Repository
public class TagRepositoryImpl extends AbstractRepository implements TagRepository {

    /**
     * Logger.
     */
    private static final Logger LOGGER = Logger.getLogger(TagRepositoryImpl.class.getName());

    /**
     * Public constructor.
     */
    public TagRepositoryImpl() {
        super(Tag.TAG);
    }

    /**
     * Tag-Article relation repository.
     */
    @Inject
    private TagArticleRepositoryImpl tagArticleRepository;

    @Override
    public JSONObject getByTitle(final String tagTitle) throws RepositoryException {
        final Query query = new Query().setFilter(
                new PropertyFilter(Tag.TAG_TITLE_LOWER_CASE, FilterOperator.EQUAL, tagTitle.toLowerCase())).
                setPageCount(1);
        try {
            final JSONObject result = get(query);
            final JSONArray array = result.getJSONArray(Keys.RESULTS);

            if (0 == array.length()) {
                return null;
            }

            return array.getJSONObject(0);
        } catch (final Exception e) {
            LOGGER.log(Level.ERROR, e.getMessage(), e);

            return null;
        }
    }

    @Override
    public List<JSONObject> getMostUsedTags(final int num) {
        final Query query = new Query().addSort(Tag.TAG_REFERENCE_COUNT,
                SortDirection.DESCENDING).
                setCurrentPageNum(1).
                setPageSize(num).setPageCount(1);

        try {
            final JSONObject result = get(query);
            final JSONArray array = result.getJSONArray(Keys.RESULTS);

            return CollectionUtils.jsonArrayToList(array);
        } catch (final Exception e) {
            LOGGER.log(Level.ERROR, e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    @Override
    public List<JSONObject> getByArticleId(final String articleId)
            throws RepositoryException {
        final List<JSONObject> ret = new ArrayList<JSONObject>();

        try {
            final List<JSONObject> tagArticleRelations = tagArticleRepository.getByArticleId(articleId);
            for (final JSONObject tagArticleRelation : tagArticleRelations) {
                final String tagId = tagArticleRelation.getString(Tag.TAG + "_" + Keys.OBJECT_ID);
                final JSONObject tag = get(tagId);

                ret.add(tag);
            }
        } catch (final JSONException e) {
            LOGGER.error(e.getMessage());
            throw new RepositoryException(e);
        }

        return ret;
    }

    /**
     * Sets the tag article repository with the specified tag article repository.
     *
     * @param tagArticleRepository the specified tag article repository
     */
    public void setTagArticleRepository(final TagArticleRepositoryImpl tagArticleRepository) {
        this.tagArticleRepository = tagArticleRepository;
    }
}
