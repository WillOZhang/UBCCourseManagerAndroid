package will.ubccoursemanager.CourseSchedule;

import android.os.Build;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Time;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import will.ubccoursemanager.CourseSchedule.CourseScheduleManager.Building;
import will.ubccoursemanager.CourseSchedule.CourseScheduleManager.BuildingManager;
import will.ubccoursemanager.CourseSchedule.CourseScheduleManager.Classroom;
import will.ubccoursemanager.CourseSchedule.CourseScheduleManager.Course;
import will.ubccoursemanager.CourseSchedule.CourseScheduleManager.CourseManager;
import will.ubccoursemanager.CourseSchedule.CourseScheduleManager.Department;
import will.ubccoursemanager.CourseSchedule.CourseScheduleManager.Exceptions.InstructorTBAException;
import will.ubccoursemanager.CourseSchedule.CourseScheduleManager.Exceptions.NoScheduledMeetingException;
import will.ubccoursemanager.CourseSchedule.CourseScheduleManager.Instructor;
import will.ubccoursemanager.CourseSchedule.CourseScheduleManager.InstructorManager;
import will.ubccoursemanager.CourseSchedule.CourseScheduleManager.Section;

public class ReadData {
    private CourseManager courseManager;
    private InstructorManager instructorManager;
    private BuildingManager buildingManager;

    public ReadData(String json) {
       try {
            JSONArray departmentsJsonArray = new JSONArray(json);
            for (int departmentIndex = 0; departmentIndex < departmentsJsonArray.length(); departmentIndex++ ) {

                JSONObject departmentJsonObject = departmentsJsonArray.getJSONObject(departmentIndex);
                Department department = new Department(departmentJsonObject.getString("shortName"));
                department.setFaculty(departmentJsonObject.getString("faculty"));
                department.setName(departmentJsonObject.getString("name"));

                JSONArray coursesArray = departmentJsonObject.getJSONArray("courses");
                for (int courseIndex = 0; courseIndex < coursesArray.length(); courseIndex++) {

                    JSONObject courseJson = coursesArray.getJSONObject(courseIndex);
                    Course course = new Course(department, courseJson.getString("courseNumber"), courseJson.getString("courseName"));
                    course.setCredits(courseJson.getString("credits"));
                    course.setDescription(courseJson.getString("description"));
                    course.setReqs(courseJson.getString("reqs"));

                    JSONArray sectionJsonArray = courseJson.getJSONArray("sections");
                    for (int sectionIndex = 0; sectionIndex < sectionJsonArray.length(); sectionIndex++) {

                        JSONObject sectionJson = sectionJsonArray.getJSONObject(sectionIndex);
                        // section json should equal to the according path

                        Section section = new Section(course, sectionJson.getString("section"), sectionJson.getString("status"),
                                sectionJson.getString("activity"), null, null, sectionJson.getString("term"));

                        try {
                            String classroomName = sectionJson.getString("classroom");
                            Building building = new Building(sectionJson.getString("building"));
                            Classroom classroom = new Classroom(classroomName, building);
                            section.setClassroom(classroom);

                            JSONArray daysJsonArray =  sectionJson.getJSONArray("days");
                            for (int daysIndex = 0; daysIndex <daysJsonArray.length(); daysIndex++) {

                                JSONArray daysJson = daysJsonArray.getJSONArray(daysIndex);
                                for (int index = 0; index < daysJson.length() - 1; index++) {
                                    JSONObject daysPair = daysJson.getJSONObject(index);
                                    Iterator iterator = daysPair.keys();
                                    String key = null;
                                    while (iterator.hasNext())
                                        key = (String) iterator.next();
                                    Time start = (Time) daysPair.get(key);
                                    Time end = (Time) daysJson.getJSONObject(index + 1).get(key);
                                    section.addTime(key, start, end);
                                }
                            }

                        } catch (JSONException ignored) {
                        }

                        try {
                            JSONObject instructorInfo = sectionJson.getJSONObject("instructor");
                            Instructor instructor = new Instructor(instructorInfo.getString("name"));
                            instructor.setWebsite(instructorInfo.getString("website"));
                            section.setInstructor(instructor);
                        } catch (JSONException ignored) {
                        }
                        course.addSection(section);
                    }
                }
                CourseManager.getInstance().addDepartment(department);
            }
       } catch (JSONException ignored) {
       }

        courseManager = CourseManager.getInstance();
        instructorManager = InstructorManager.getInstance();
        buildingManager = BuildingManager.getInstance();
    }

    public CourseManager getCourseManager() {
        return courseManager;
    }

    public InstructorManager getInstructorManager() {
        return instructorManager;
    }

    public BuildingManager getBuildingManager() {
        return buildingManager;
    }
}
