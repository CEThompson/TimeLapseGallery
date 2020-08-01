package com.vwoom.timelapsegallery.data.repository.fakes

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.vwoom.timelapsegallery.data.entry.ProjectTagEntry
import com.vwoom.timelapsegallery.data.entry.TagEntry
import com.vwoom.timelapsegallery.data.repository.ITagRepository
import com.vwoom.timelapsegallery.data.view.ProjectView

class FakeTagRepository : ITagRepository {

    val fakeProjectTags: ArrayList<ProjectTagEntry> = arrayListOf(
            ProjectTagEntry(id = 0, project_id = 0, tag_id = 0),
            ProjectTagEntry(id = 1, project_id = 1, tag_id = 1),
            ProjectTagEntry(id = 2, project_id = 2, tag_id = 2),
            ProjectTagEntry(id = 3, project_id = 3, tag_id = 3)
    )

    val fakeTags: ArrayList<TagEntry> = arrayListOf(
            TagEntry(id = 0, text = "zero"),
            TagEntry(id = 1, text = "one"),
            TagEntry(id = 2, text = "two"),
            TagEntry(id = 3, text = "three")
    )

    override suspend fun getProjectTags(projectId: Long): List<ProjectTagEntry> {
        val result = arrayListOf<ProjectTagEntry>()
        for (projectTag in fakeProjectTags){
            if (projectTag.project_id == projectId) result.add(projectTag)
        }
        //println("project tags for project id $projectId is $result")
        return result
    }

    override fun getProjectTagsLiveData(projectId: Long): LiveData<List<ProjectTagEntry>> {
        val result = arrayListOf<ProjectTagEntry>()
        for (projectTag in fakeProjectTags){
            if (projectTag.project_id == projectId) result.add(projectTag)
        }
        //println("live data tags for project id $projectId is $result")
        return MutableLiveData(result)
    }

    override fun getTagsLiveData(): LiveData<List<TagEntry>> {
        return MutableLiveData(fakeTags)
    }

    override suspend fun getTagsFromProjectTags(projectTags: List<ProjectTagEntry>): List<TagEntry> {
        val result: ArrayList<TagEntry> = arrayListOf()
        for (projectTag in projectTags) {
            for (tag in fakeTags) {
                if (projectTag.tag_id == tag.id) {
                    result.add(tag)
                    break
                }
            }
        }
        //println("tags from project tags are $result")
        return result
    }


    override suspend fun deleteTag(tagEntry: TagEntry) {
        for (tag in fakeTags) {
            if (tag.id == tagEntry.id) {
                fakeTags.remove(tag)
                break
            }
        }
    }

    override suspend fun deleteTagFromProject(tagEntry: TagEntry, projectView: ProjectView) {
        for (projectTag in fakeProjectTags) {
            if (projectTag.project_id == projectView.project_id) {
                fakeProjectTags.remove(projectTag)
            }
        }
    }

    override suspend fun addTagToProject(tagText: String, projectView: ProjectView) {
        // If project tag already exists return
        for (projectTag in fakeProjectTags) {
            if (projectTag.project_id == projectView.project_id) return
        }

        var tag: TagEntry? = null
        for (currentTag in fakeTags) {
            if (currentTag.text == tagText) {
                tag = currentTag
            }
        }

        // Add the tag if it doesnt exit
        if (tag == null) {
            val newTag = TagEntry(id = getNextTagId(), text = tagText)
            fakeTags.add(newTag)
            tag = newTag
        }

        // Add the project tag link
        val newProjectTag = ProjectTagEntry(id = getNextProjectTagId(), tag_id = tag.id, project_id = projectView.project_id)
        fakeProjectTags.add(newProjectTag)
    }

    private fun getNextProjectTagId(): Long {
        return (fakeProjectTags.last().id+1)
    }

    private fun getNextTagId(): Long {
        return (fakeTags.last().id + 1)
    }



}