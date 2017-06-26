package will.ubccoursemanager.CourseSchedule;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.io.Reader;
import java.net.URL;
import java.nio.charset.Charset;
import java.sql.Time;

import will.ubccoursemanager.CourseSchedule.CourseScheduleManager.Building;
import will.ubccoursemanager.CourseSchedule.CourseScheduleManager.BuildingManager;
import will.ubccoursemanager.CourseSchedule.CourseScheduleManager.Classroom;
import will.ubccoursemanager.CourseSchedule.CourseScheduleManager.Course;
import will.ubccoursemanager.CourseSchedule.CourseScheduleManager.CourseManager;
import will.ubccoursemanager.CourseSchedule.CourseScheduleManager.Department;
import will.ubccoursemanager.CourseSchedule.CourseScheduleManager.InstructorManager;
import will.ubccoursemanager.CourseSchedule.CourseScheduleManager.Section;

/**
 * Created by Will on 2017/6/25.
 */

public class RequestData {
    public static final String OBJECT_PATH = "/Users/ZC/Documents/CourseScheduleBackground/src/com/CourseSchedule/CourseScheduleManager/";
    public static final String PREFIX = "https://courses.students.ubc.ca";
    private static Section lastSection;
    private int count = 0;
    private Elements temp1;
    private Elements temp2;
    private Elements temp3;
    private Elements temp4;
    private Elements temp5;
    private int index;
    private static Elements visited;

    public RequestData(int threadNum) throws IOException {
        String text = reading(PREFIX + "/cs/main?pname=subjarea&tname=subjareas&req=0");
        Document doc = Jsoup.parse(text);
        Elements tbodyElements = doc.getElementsByTag("tbody");
        Element allDepartments = tbodyElements.first();
        Elements departments = allDepartments.getElementsByTag("tr");

        // codes for multithread
        temp1 = new Elements();
        temp2 = new Elements();
        temp3 = new Elements();
        temp4 = new Elements();
        temp5 = new Elements();
        switch (threadNum) {
            case (1):
                for (int j = 0; j < 50; j++)
                    temp1.add(departments.get(j));
                break;
            case (2):
                for (int j = 50; j < 100; j++)
                    temp2.add(departments.get(j));
                break;
            case (3):
                for (int j = 100; j < 150; j++)
                    temp3.add(departments.get(j));
                break;
            case (4):
                for (int j = 150; j < departments.size(); j++)
                    temp4.add(departments.get(j));
                break;
//            case (5):
//                for (int j = 200; j < departments.size(); j++)
//                    temp5.add(departments.get(j));
//                break;
        }
        index = threadNum;
        visited = new Elements();

        // codes for signal-thread
//        for (Element department : departments)
//            getInfoFromDepartment(department);
    }

    private synchronized void getInfoFromDepartment(Element department) throws IOException {
        count++;
        Elements elements = department.getElementsByTag("td");
        Element shortNameElement = department.getElementsByTag("a").first();
        Element departmentNameElement = elements.get(1);
        Element facultyNameElement = elements.get(2);
        if (shortNameElement != null &&
                departmentNameElement != null && facultyNameElement != null) {
            String shortName = shortNameElement.text();
            String departmentName = departmentNameElement.text();
            String facultyName = facultyNameElement.text();
            Department tempDepartment = new Department(shortName);
            tempDepartment.setName(departmentName);
            tempDepartment.setFaculty(facultyName);
            CourseManager.getInstance().addDepartmentForDownloadData(tempDepartment);

            String urlForCoursesList = shortNameElement.attr("href");
            String coursesListText = reading(PREFIX + urlForCoursesList);
            Document coursesListDoc = Jsoup.parse(coursesListText);
            Elements tbodyElements = coursesListDoc.getElementsByTag("tbody");
            Element coursesListElement = tbodyElements.first();

            handleCoursesListElement(coursesListElement);
            System.out.println(count); // TODO
        }
    }

    private synchronized void handleCoursesListElement(Element coursesListElement) throws IOException {
        Elements coursesElements = coursesListElement.getElementsByTag("tr");
        for (Element courseElement : coursesElements) {
            count++; // counting

            String course = courseElement.getElementsByTag("a").first().text();
            String[] parts = course.split(" ");
            String part1 = parts[0]; // Department
            String part2 = parts[1]; // Number
            String courseTitle = courseElement.getElementsByTag("td").get(1).text();
            CourseManager.getInstance().addCourse(part1, part2, courseTitle);
            Course editingCourse = CourseManager.getInstance().getCourse(part1, part2);

            String urlForSectionList = courseElement.getElementsByTag("a").attr("href");
            String sectionListText = reading(PREFIX + urlForSectionList);
            Document sectionListDoc = Jsoup.parse(sectionListText);

            handleSectionListDoc(sectionListDoc, editingCourse);
        }
    }

    private synchronized void handleSectionListDoc(Document sectionListDoc, Course editingCourse) throws IOException {
        Element mainContentElement = sectionListDoc.getElementsByClass("content expand").first();
        count++;

        if (editingCourse != null) {
            Elements creditsAndDescription = mainContentElement.getElementsByTag("p");
            Element description = creditsAndDescription.get(0);
            Element credits = creditsAndDescription.get(1);
            editingCourse.setCredits(credits.text());
            editingCourse.setDescription(description.text());

            // dealing with reqs
            String reqs = "";
            for (int i = 2; i < creditsAndDescription.size(); i++)
                reqs += creditsAndDescription.get(i).text() + "\n";
            editingCourse.setReqs(reqs);

            // dealing with sections
            Element sectionTableElement = mainContentElement.getElementsByTag("tbody").first();
            Elements sectionElements = sectionTableElement.getElementsByTag("tr");
            for (Element section : sectionElements)
                handleEachSection(editingCourse, section);
        }
    }

    private synchronized void handleEachSection(Course editingCourse, Element section) throws IOException {
        count++;
        Elements sectionInfo = section.getElementsByTag("td");
        String status = sectionInfo.get(0).text();
        String courseFullInfo = sectionInfo.get(1).text();
        if (!courseFullInfo.equals("") && !courseFullInfo.equals(" ")) {
            String Section = sectionInfo.get(1).text().split(" ")[2];
            String activity = sectionInfo.get(2).text();
            String term = sectionInfo.get(3).text();

            String urlForSectionInfo = sectionInfo.get(1).getElementsByTag("a").attr("href");
            String sectionInfoText = reading(PREFIX + urlForSectionInfo);
            count++;
            Document sectionInfoDoc = Jsoup.parse(sectionInfoText);
            Element sectionPage = sectionInfoDoc.getElementsByClass("content expand").first();

            // dealing with last day to withdraw
            Element withdrawDay = sectionPage.getElementsByClass("table table-nonfluid").first();
            Element withdrawInfoElement = withdrawDay.getElementsByTag("tbody").first();
            String withdrawInfo = withdrawInfoElement.text();

            // dealing with seats
            Element seatsSummaryElement = sectionPage.getElementsByAttribute("table-nonfluid&#39;").first();
            int total = 0;
            int current = 0;
            int general = 0;
            int restricted = 0;
            String restrictedTo = "";
            if (seatsSummaryElement != null) {
                Element seatsInfoElement = seatsSummaryElement.getElementsByTag("tbody").first();
                String seatsInfoCombine = getSeatsInfo(seatsInfoElement);
                try {
                    String[] seatsInfoArray = seatsInfoCombine.split("@");
                    total = Integer.parseInt(seatsInfoArray[0]);
                    current = Integer.parseInt(seatsInfoArray[1]);
                    general = Integer.parseInt(seatsInfoArray[2]);
                    restricted = Integer.parseInt(seatsInfoArray[3]);
                    if (seatsInfoArray.length > 4)
                        restrictedTo = seatsInfoArray[4];
                } catch (IndexOutOfBoundsException | NumberFormatException e) {
                    System.out.println("IndexOutOfBoundsException | NumberFormatException is found in " +
                            editingCourse.getDepartment().getShortName() + editingCourse.getCourseNumber()
                            + sectionInfoText);
                }
            }

            // dealing with classroom info
            Element classroomInfo = sectionPage.getElementsByClass("table  table-striped").first();
            Classroom classroom1;
            if (classroomInfo != null) {
                Elements buildingInfoTable = classroomInfo.getElementsByTag("td");
                String building = buildingInfoTable.get(4).text();
                String classroom = buildingInfoTable.get(5).text();
                Building building1;
                if (building.equals("No Scheduled Meeting"))
                    classroom1 = null;
                else {
                    building1 = new Building(building);
                    classroom1 = new Classroom(classroom, building1);
                    Building temp = BuildingManager.getInstance().getBuilding(building1);
                    temp.addClassroom(classroom1);
                }
            } else
                classroom1 = null;

            // dealing with instructor info
            Elements info = sectionPage.getElementsByTag("table");
            Elements instructorInfoElements = findingInstructorInfo(info);
            String instructorInfo;
            if (instructorInfoElements != null)
                instructorInfo = instructorInfoElements.text().split(": ")[1];
            else
                instructorInfo = null;
            Instructor instructor;
            if (instructorInfo == null || instructorInfo.contains("TBA"))
                instructor = null;
            else {
                Instructor temp = new Instructor(instructorInfo);
                instructor = InstructorManager.getInstance().getInstructor(temp);
                instructor.addCourse(editingCourse);
                Element tempWeb = instructorInfoElements.tagName("a").first().getElementsByTag("a").get(1);
                String website = tempWeb.attr("href");
                instructor.setWebsite(website);
            }

            // ignore all waiting list courses and the sections that are blocked
            if (activity.equals("Waiting List"))
                return;
            if (status.equals("Blocked"))
                return;

            // Set the section
            Section currentSection = new Section(editingCourse, Section, status, activity, instructor, classroom1, term);
            currentSection.setLastWithdraw(withdrawInfo);
            currentSection.setSeatsInfo(total, current, restricted, general);
            currentSection.setRestrictTo(restrictedTo);
            editingCourse.addSection(currentSection);
            if (instructor != null)
                instructor.addSection(currentSection);
            lastSection = currentSection;

            String days = sectionInfo.get(5).text();
            try {
                if (!days.equals("") && !days.equals(" ")
                        && classroom1 != null) { // in some cases, days are available and yet time is not
                    String[] time1 = sectionInfo.get(6).text().split(":");
                    Time start = new Time(Integer.parseInt(time1[0]), Integer.parseInt(time1[1]), 0);
                    String[] time2 = sectionInfo.get(7).text().split(":");
                    Time end = new Time(Integer.parseInt(time2[0]), Integer.parseInt(time2[1]), 0);
                    currentSection.addTime(days, start, end);
                }
            } catch (NumberFormatException e) {

            }
        } else { // if one section has separated colums storing info
            String days = sectionInfo.get(5).text();
            try {
                lastSection.getClassroom();
                if (!days.equals("") && !days.equals(" ")) { // add try catch to avoid NumberFormatException
                    String[] time1 = sectionInfo.get(6).text().split(":");
                    Time start = new Time(Integer.parseInt(time1[0]), Integer.parseInt(time1[1]), 0);
                    String[] time2 = sectionInfo.get(7).text().split(":");
                    Time end = new Time(Integer.parseInt(time2[0]), Integer.parseInt(time2[1]), 0);
                    lastSection.addTime(days, start, end);
                }
            } catch (NoScheduledMeetingException e) {
                return;
            } catch (NumberFormatException e) {

            }
        }
    }

    private String getSeatsInfo(Element seatsInfoElement) {
        String temp = "";
        String split = "@";
        String total = "";
        String current = "";
        String general = "";
        String restricted = "";
        String restrictedTo = "";
        Elements tempInfo = seatsInfoElement.getElementsByTag("tr");
        for (Element element : tempInfo) {
            if (element.text().contains("Total"))
                total = element.getElementsByTag("strong").first().text();
            else if (element.text().contains("Current"))
                current = element.getElementsByTag("strong").first().text();
            else if (element.text().contains("General"))
                general = element.getElementsByTag("strong").first().text();
            else if (element.text().contains("Restricted"))
                restricted = element.getElementsByTag("strong").first().text();
            else
                restrictedTo = element.text();
        }
        temp = total + split +
                current + split +
                general + split +
                restricted + split + restrictedTo;

        return temp;
    }

    private static Elements findingInstructorInfo(Elements info) {
        for (Element element : info) {
            Elements childNodes = element.children().tagName("td").tagName("td");
            if (childNodes.text().contains("Instructor"))
                return childNodes;
        }
        return null;
    }

    private static String readAll(Reader rd) throws IOException {
        StringBuilder sb = new StringBuilder();
        int cp;
        while ((cp = rd.read()) != -1) {
            sb.append((char) cp);
        }
        return sb.toString();
    }

    public static String reading(String url) throws IOException {
        InputStream is = new URL(url).openStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")));

        return readAll(reader);
    }

    @Override
    public void run() {
        try {
            switch (index) {
                case (1):
                    for (Element element : temp1) {
                        getInfoFromDepartment(element);
                        visited.add(element);
                    }
                    break;
                case (2):
                    for (Element element : temp2) {
                        getInfoFromDepartment(element);
                        visited.add(element);
                    }
                    break;
                case (3):
                    for (Element element : temp3) {
                        getInfoFromDepartment(element);
                        visited.add(element);
                    }
                    break;
                case (4):
                    for (Element element : temp4) {
                        getInfoFromDepartment(element);
                        visited.add(element);
                    }
                    break;
//                case (5):
//                    for (Element element : temp5) {
//                        getInfoFromDepartment(element);
//                        visited.add(element);
//                    }
//                    break;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        long start = System.currentTimeMillis();
        ModifyMultithreadedRequestData test1 = new ModifyMultithreadedRequestData(1);
        ModifyMultithreadedRequestData test2 = new ModifyMultithreadedRequestData(2);
        ModifyMultithreadedRequestData test3 = new ModifyMultithreadedRequestData(3);
        ModifyMultithreadedRequestData test4 = new ModifyMultithreadedRequestData(4);
        //ModifyMultithreadedRequestData test5 = new ModifyMultithreadedRequestData(5);
        Thread[] threads = new Thread[5];
        threads[0] = new Thread(test1);
        threads[0].start();
        threads[1] = new Thread(test2);
        threads[1].start();
        threads[2] = new Thread(test3);
        threads[2].start();
        threads[3] = new Thread(test4);
        threads[3].start();
//        threads[4] = new Thread(test5);
//        threads[4].start();

        for (int i = 0; i < threads.length; i++)
            if (threads[i] != null)
                threads[i].join();

        // storing manger objects
        serializeManagers();
        long end = System.currentTimeMillis();
        long time = end - start;
        System.out.println("Time: " + time);
    }

    public static void serializeManagers() {
        FileOutputStream BMFout = null;
        FileOutputStream CMFout = null;
        FileOutputStream IMFout = null;
        ObjectOutputStream BMOos = null;
        ObjectOutputStream CMOos = null;
        ObjectOutputStream IMOos = null;

        try {
            BMFout = new FileOutputStream(OBJECT_PATH + "BuildingManager.ser");
            BMOos = new ObjectOutputStream(BMFout);
            BMOos.writeObject(BuildingManager.getInstance());

            CMFout = new FileOutputStream(OBJECT_PATH + "CourseManager.ser");
            CMOos = new ObjectOutputStream(CMFout);
            CMOos.writeObject(CourseManager.getInstance());

            IMFout = new FileOutputStream(OBJECT_PATH + "InstructorManager.ser");
            IMOos = new ObjectOutputStream(IMFout);
            IMOos.writeObject(InstructorManager.getInstance());

            System.out.println("Done");

        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            if (BMFout != null && CMFout != null && IMFout != null) {
                try {
                    BMFout.close();
                    CMFout.close();
                    IMFout.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (BMOos != null && CMOos != null && IMOos != null) {
                try {
                    BMOos.close();
                    CMFout.close();
                    IMFout.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
