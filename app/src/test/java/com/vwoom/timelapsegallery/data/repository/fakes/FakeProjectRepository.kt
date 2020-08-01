package com.vwoom.timelapsegallery.data.repository.fakes

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.vwoom.timelapsegallery.data.entry.PhotoEntry
import com.vwoom.timelapsegallery.data.entry.ProjectScheduleEntry
import com.vwoom.timelapsegallery.data.repository.IProjectRepository
import com.vwoom.timelapsegallery.data.view.ProjectView
import java.io.File

class FakeProjectRepository : IProjectRepository {

    val fakeProjects: ArrayList<ProjectView> = arrayListOf(
            ProjectView(project_id = 0, project_name = "zero", interval_days = 0, cover_photo_id = 0, cover_photo_timestamp = 123456789),
            ProjectView(project_id = 1, project_name = "one", interval_days = 1, cover_photo_id = 1, cover_photo_timestamp = 123456789),
            ProjectView(project_id = 2, project_name = "two", interval_days = 2, cover_photo_id = 2, cover_photo_timestamp = 123456789),
            ProjectView(project_id = 3, project_name = "three", interval_days = 3, cover_photo_id = 3, cover_photo_timestamp = 123456789)
    )

    val fakePhotos: ArrayList<PhotoEntry> = arrayListOf(
            PhotoEntry(id = 0, project_id = 0, timestamp = 123456789),
            PhotoEntry(id = 1, project_id = 1, timestamp = 123456789),
            PhotoEntry(id = 2, project_id = 2, timestamp = 123456789),
            PhotoEntry(id = 3, project_id = 3, timestamp = 123456789)
    )


    // Getters
    override fun getProjectPhotosLiveData(projectId: Long): LiveData<List<PhotoEntry>> {
        return MutableLiveData(fakePhotos)
    }

    override fun getProjectViewLiveData(projectId: Long): LiveData<ProjectView?> {
        // TODO figure this out
        var projectView: ProjectView? = null
        for (fakeProject in fakeProjects) {
            if (fakeProject.project_id == projectId) {
                projectView = fakeProject
                break
            }
        }
        return MutableLiveData(projectView)
    }

    override fun getProjectViewsLiveData(): LiveData<List<ProjectView>> {
        return MutableLiveData<List<ProjectView>>(fakeProjects)
    }

    override fun getScheduledProjectViews(): List<ProjectView> {
        return fakeProjects.filter { it.interval_days > 0 }
    }


    // Project management
    override suspend fun deleteProject(externalFilesDir: File, projectId: Long) {
        for (project in fakeProjects) {
            if (project.project_id == projectId) {
                fakeProjects.remove(project)
                break
            }
        }
    }

    override suspend fun newProject(file: File, externalFilesDir: File, timestamp: Long, scheduleInterval: Int): ProjectView {
        val projectId = getNextProjectId()
        val photoId = getNextPhotoId()
        val photoToAdd = PhotoEntry(id = photoId, project_id = projectId, timestamp = timestamp)
        val projectToAdd = ProjectView(
                project_id = getNextProjectId(),
                project_name = "newProject",
                interval_days = scheduleInterval,
                cover_photo_id = photoToAdd.id,
                cover_photo_timestamp = photoToAdd.timestamp)

        // Add the project and the photo
        fakeProjects.add(projectToAdd)
        fakePhotos.add(photoToAdd)

        return projectToAdd
    }


    override suspend fun setProjectCoverPhoto(entry: PhotoEntry) {
        val projectId = entry.project_id
        for (project in fakeProjects) {
            if (project.project_id == projectId) {
                val index = fakeProjects.indexOf(project)
                val updatedProject = ProjectView(
                        project_id = project.project_id,
                        project_name = project.project_name,
                        interval_days = project.interval_days,
                        cover_photo_id = entry.id,
                        cover_photo_timestamp = entry.timestamp
                )
                fakeProjects.add(index, updatedProject)
                fakeProjects.remove(project)
                break
            }
        }
    }

    override suspend fun setProjectSchedule(externalFilesDir: File, projectView: ProjectView, projectScheduleEntry: ProjectScheduleEntry) {
        val projectId = projectScheduleEntry.project_id
        for (project in fakeProjects) {
            if (project.project_id == projectId) {
                val index = fakeProjects.indexOf(project)
                val updatedProject = ProjectView(
                        project_id = project.project_id,
                        project_name = project.project_name,
                        interval_days = projectScheduleEntry.interval_days,
                        cover_photo_id = project.cover_photo_id,
                        cover_photo_timestamp = project.cover_photo_timestamp
                )
                fakeProjects.add(index, updatedProject)
                fakeProjects.remove(project)
                break
            }
        }
    }

    override suspend fun updateProjectName(externalFilesDir: File, sourceProjectView: ProjectView, name: String) {
        val projectId = sourceProjectView.project_id
        for (project in fakeProjects) {
            if (project.project_id == projectId) {
                val index = fakeProjects.indexOf(project)
                val updatedProject = ProjectView(
                        project_id = project.project_id,
                        project_name = name,
                        interval_days = project.interval_days,
                        cover_photo_id = project.cover_photo_id,
                        cover_photo_timestamp = project.cover_photo_timestamp
                )
                fakeProjects.add(index, updatedProject)
                fakeProjects.remove(project)
                break
            }
        }
    }

    // Photo management
    override suspend fun addPhotoToProject(file: File, externalFilesDir: File, projectView: ProjectView, timestamp: Long) {
        val photoToAdd = PhotoEntry(
                id = getNextPhotoId(),
                project_id = projectView.project_id,
                timestamp = timestamp
        )
        fakePhotos.add(photoToAdd)
    }

    override suspend fun deleteProjectPhoto(externalFilesDir: File, photoEntry: PhotoEntry) {
        for (photo in fakePhotos) {
            if (photo.id == photoEntry.id) {
                fakePhotos.remove(photo)
                break
            }
        }
    }


    // Helpers
    private fun getNextPhotoId(): Long {
        val lastPhoto = fakePhotos.last()
        return (lastPhoto.id + 1)
    }

    private fun getNextProjectId(): Long {
        val lastProject = fakeProjects.last()
        return (lastProject.project_id + 1)
    }
}