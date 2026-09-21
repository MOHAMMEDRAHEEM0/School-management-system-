package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.withTransaction
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.local.dao.SchoolDao
import com.example.data.local.entities.AcademicClassEntity
import com.example.data.local.entities.AdmissionApplicationEntity
import com.example.data.local.entities.AttendanceEntity
import com.example.data.local.entities.AttendanceRecordEntity
import com.example.data.local.entities.AuditLogEntity
import com.example.data.local.entities.CertificateEntity
import com.example.data.local.entities.CircularEntity
import com.example.data.local.entities.ExamRecordEntity
import com.example.data.local.entities.FeeRecordEntity
import com.example.data.local.entities.GovernmentComplianceEntity
import com.example.data.local.entities.IdCardRecordEntity
import com.example.data.local.entities.InchargeEditRequestEntity
import com.example.data.local.entities.InstitutionEntity
import com.example.data.local.entities.InstitutionalBillEntity
import com.example.data.local.entities.NoticeMeetingEntity
import com.example.data.local.entities.RegisterScanBatchEntity
import com.example.data.local.entities.SchoolDiaryEntity
import com.example.data.local.entities.SchoolInstructionEntity
import com.example.data.local.entities.StaffEntity
import com.example.data.local.entities.StudentEntity
import com.example.data.local.entities.SyncQueueEntity
import com.example.data.local.entities.TeacherEntity
import com.example.data.local.entities.TimetableSlotEntity
import com.example.data.local.entities.UserEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        InstitutionEntity::class,
        AcademicClassEntity::class,
        UserEntity::class,
        TeacherEntity::class,
        StaffEntity::class,
        StudentEntity::class,
        AttendanceEntity::class,
        AttendanceRecordEntity::class,
        RegisterScanBatchEntity::class,
        ExamRecordEntity::class,
        FeeRecordEntity::class,
        CertificateEntity::class,
        IdCardRecordEntity::class,
        AdmissionApplicationEntity::class,
        TimetableSlotEntity::class,
        NoticeMeetingEntity::class,
        CircularEntity::class,
        SchoolDiaryEntity::class,
        SchoolInstructionEntity::class,
        InstitutionalBillEntity::class,
        GovernmentComplianceEntity::class,
        SyncQueueEntity::class,
        InchargeEditRequestEntity::class,
        AuditLogEntity::class
    ],
    version = 5,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun schoolDao(): SchoolDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(
            context: Context,
            scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
        ): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "st_xaviers_sms_master.db"
                )
                    .fallbackToDestructiveMigration()
                    .addCallback(DatabaseCallback(scope))
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class DatabaseCallback(
        private val scope: CoroutineScope
    ) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                scope.launch(Dispatchers.IO) {
                    populateInitialData(database)
                }
            }
        }

        private suspend fun populateInitialData(database: AppDatabase) {
            database.withTransaction {
                val dao = database.schoolDao()

                // 1. Pre-fed Registered Institutions (Supporting Real Registration Lookup & Search)
                val registeredInstitutions = listOf(
                    InstitutionEntity(
                        institutionId = "INST-SXA-2026",
                        registrationCode = "3477",
                        name = "St. Xavier's International Academy",
                        motto = "Excellence in Truth & Service",
                        affiliationCode = "CBSE Affil: 1130492 | ISO 9001:2026",
                        address = "Campus Road, Civil Lines, Sector 5",
                        cityCampus = "Trimurtinagar, Ahmednagar & Sector 5",
                        contactPhone = "+91 98111 22334",
                        email = "office@stxaviers.edu",
                        website = "https://stxaviers.edu",
                        currentAcademicYear = "2026-2027",
                        availableBatches = "2026-27,2025-26,2024-25",
                        principalName = "Prof. Robert Vance, Ph.D.",
                        themeColorHex = "#0B2545",
                        logoAssetPath = "ic_school_crest",
                        establishedYear = 1984,
                        isSetupCompleted = true
                    ),
                    InstitutionEntity(
                        institutionId = "INST-HG-4104",
                        registrationCode = "4104",
                        name = "H.G. International School",
                        motto = "Knowledge, Discipline, Character",
                        affiliationCode = "State Board Affil No: 4104-RJ",
                        address = "Palanpur Highway, Mount Road",
                        cityCampus = "Sirohi, Rajasthan",
                        contactPhone = "+91 94140 12345",
                        email = "info@hginternational.org",
                        website = "https://hginternational.org",
                        currentAcademicYear = "2026-2027",
                        availableBatches = "2026-27,2025-26",
                        principalName = "Dr. Virendra Rathore",
                        themeColorHex = "#1E3A8A",
                        logoAssetPath = "ic_school_crest",
                        establishedYear = 1998,
                        isSetupCompleted = true
                    ),
                    InstitutionEntity(
                        institutionId = "INST-SN-3767",
                        registrationCode = "3767",
                        name = "Shree Narayana Higher Secondary School",
                        motto = "Lead Kindly Light",
                        affiliationCode = "GSEB Affil No: 3767-GJ",
                        address = "Jodhpur Gam Road, Satellite",
                        cityCampus = "Ahmedabad, Gujarat",
                        contactPhone = "+91 98250 88990",
                        email = "contact@shreenarayana.edu",
                        website = "https://shreenarayana.edu",
                        currentAcademicYear = "2026-2027",
                        availableBatches = "2026-27,2025-26",
                        principalName = "Smt. Kalyani Menon",
                        themeColorHex = "#047857",
                        logoAssetPath = "ic_school_crest",
                        establishedYear = 1982,
                        isSetupCompleted = true
                    ),
                    InstitutionEntity(
                        institutionId = "INST-SI-3825",
                        registrationCode = "3825",
                        name = "Shanti International School",
                        motto = "Inspiring Minds, Securing Futures",
                        affiliationCode = "CBSE Affil No: 3825-GJ",
                        address = "Radhanpur Road Bypass",
                        cityCampus = "Mehsana, Gujarat",
                        contactPhone = "+91 99099 44556",
                        email = "admissions@shanti-mehsana.edu",
                        website = "https://shanti-mehsana.edu",
                        currentAcademicYear = "2026-2027",
                        availableBatches = "2026-27,2025-26",
                        principalName = "Dr. Harish Patel",
                        themeColorHex = "#B45309",
                        logoAssetPath = "ic_school_crest",
                        establishedYear = 2005,
                        isSetupCompleted = true
                    ),
                    InstitutionEntity(
                        institutionId = "INST-AS-4059",
                        registrationCode = "4059",
                        name = "AS Vidyapeeth Academy",
                        motto = "Wisdom through Dedication",
                        affiliationCode = "HP Board Affil No: 4059-HP",
                        address = "The Mall Road, Near Ridge",
                        cityCampus = "Shimla, Himachal Pradesh",
                        contactPhone = "+91 98160 33221",
                        email = "shimla@asvidyapeeth.org",
                        website = "https://asvidyapeeth.org",
                        currentAcademicYear = "2026-2027",
                        availableBatches = "2026-27,2025-26",
                        principalName = "Col. R.K. Sharma",
                        themeColorHex = "#4C1D95",
                        logoAssetPath = "ic_school_crest",
                        establishedYear = 1992,
                        isSetupCompleted = true
                    ),
                    InstitutionEntity(
                        institutionId = "INST-RP-3796",
                        registrationCode = "3796",
                        name = "Rotary Public School",
                        motto = "Service Above Self",
                        affiliationCode = "ICSE Affil No: 3796-OD",
                        address = "Khandagiri Square, Unit 8",
                        cityCampus = "Bhubaneswar, Odisha",
                        contactPhone = "+91 94370 77889",
                        email = "office@rotarypublicodisha.org",
                        website = "https://rotarypublicodisha.org",
                        currentAcademicYear = "2026-2027",
                        availableBatches = "2026-27,2025-26",
                        principalName = "Mrs. Anita Patnaik",
                        themeColorHex = "#0369A1",
                        logoAssetPath = "ic_school_crest",
                        establishedYear = 1989,
                        isSetupCompleted = true
                    ),
                    InstitutionEntity(
                        institutionId = "INST-AP-4120",
                        registrationCode = "4120",
                        name = "Abhishek Public Sr. Sec. School",
                        motto = "Empowering Youth, Building Nations",
                        affiliationCode = "HPBSE Affil No: 4120-KG",
                        address = "Dharamshala Road",
                        cityCampus = "Kangra, Himachal Pradesh",
                        contactPhone = "+91 98050 66554",
                        email = "principal@abhishekpublic.edu",
                        website = "https://abhishekpublic.edu",
                        currentAcademicYear = "2026-2027",
                        availableBatches = "2026-27,2025-26",
                        principalName = "Er. Sanjeev Mahajan",
                        themeColorHex = "#7C2D12",
                        logoAssetPath = "ic_school_crest",
                        establishedYear = 2001,
                        isSetupCompleted = true
                    )
                )
                dao.insertInstitutions(registeredInstitutions)

                // 2. Pre-fed Academic Structure (Nursery, LKG, UKG, Classes 1 to 10 with sections A, B, C, D)
                val academicClasses = mutableListOf<AcademicClassEntity>()
                val classNames = listOf(
                    "Nursery", "LKG", "UKG",
                    "Class 1", "Class 2", "Class 3", "Class 4", "Class 5",
                    "Class 6", "Class 7", "Class 8", "Class 9", "Grade 10"
                )
                classNames.forEach { className ->
                    listOf("A", "B", "C", "D").forEach { sec ->
                        val id = "CLS-${className.replace(" ", "").uppercase()}-$sec"
                        val teacherName = if (className == "Grade 10" && sec == "A") "Mrs. Sunita Kapoor" else "Senior Faculty"
                        val teacherId = if (className == "Grade 10" && sec == "A") "FAC-TCH-02" else null
                        academicClasses.add(
                            AcademicClassEntity(
                                classId = id,
                                className = className,
                                section = sec,
                                classTeacherId = teacherId,
                                classTeacherName = teacherName,
                                roomNumber = if (className == "Grade 10" && sec == "A") "Room 304 - Secondary Wing" else "Wing B",
                                studentCount = 30,
                                academicYear = "2026-2027"
                            )
                        )
                    }
                }
                dao.insertAcademicClasses(academicClasses)

                // 3. Pre-fed Core RBAC Users
                val initialUsers = listOf(
                    UserEntity(
                        id = "USR-SADM-01",
                        institutionId = "INST-SXA-2026",
                        username = "super_admin",
                        email = "superadmin@stxaviers.edu",
                        password = "Password@123",
                        role = "PRINCIPAL",
                        displayName = "Executive Trust Directorate (Super Admin)",
                        phoneNumber = "9800000001",
                        authPin = "9876",
                        isOtpVerified = true
                    ),
                    UserEntity(
                        id = "USR-PRIN-01",
                        institutionId = "INST-SXA-2026",
                        username = "principal",
                        email = "principal@stxaviers.edu",
                        password = "Password@123",
                        role = "PRINCIPAL",
                        displayName = "Prof. Robert Vance, Ph.D. (Principal)",
                        phoneNumber = "9811122334",
                        authPin = "9876",
                        isOtpVerified = true
                    ),
                    UserEntity(
                        id = "USR-INCH-01",
                        institutionId = "INST-SXA-2026",
                        username = "incharge_10a",
                        email = "sunita.kapoor@stxaviers.edu",
                        password = "Password@123",
                        role = "INCHARGE",
                        displayName = "Mrs. Sunita Kapoor (Class In-Charge 10-A)",
                        phoneNumber = "9876543211",
                        authPin = "1234",
                        isOtpVerified = true
                    ),
                    UserEntity(
                        id = "USR-TCH-01",
                        institutionId = "INST-SXA-2026",
                        username = "faculty_meenakshi",
                        email = "m.sundaram@stxaviers.edu",
                        password = "Password@123",
                        role = "TEACHING",
                        displayName = "Dr. Meenakshi Sundaram (Physics Faculty)",
                        phoneNumber = "9876543210",
                        authPin = "1234",
                        isOtpVerified = true
                    ),
                    UserEntity(
                        id = "USR-STU-01",
                        institutionId = "INST-SXA-2026",
                        username = "aarav_sharma",
                        email = "aarav.sharma@stxaviers.edu",
                        password = "Password@123",
                        role = "STUDENT",
                        displayName = "Aarav Sharma (Scholar Roll #1)",
                        phoneNumber = "9123456780",
                        authPin = "1234",
                        isOtpVerified = true
                    ),
                    UserEntity(
                        id = "USR-PAR-01",
                        institutionId = "INST-SXA-2026",
                        username = "vikram_sharma",
                        email = "vikram.sharma@stxaviers.edu",
                        password = "Password@123",
                        role = "PARENT",
                        displayName = "Vikram Sharma (Parent of Aarav)",
                        phoneNumber = "9123456780",
                        authPin = "1234",
                        isOtpVerified = true
                    ),
                    UserEntity(
                        id = "USR-ACC-01",
                        institutionId = "INST-SXA-2026",
                        username = "accountant_mehta",
                        email = "accounts@stxaviers.edu",
                        password = "Password@123",
                        role = "ACCOUNTANT",
                        displayName = "Suresh Mehta (Accounts & Bursar)",
                        phoneNumber = "9844455667",
                        authPin = "1234",
                        isOtpVerified = true
                    ),
                    UserEntity(
                        id = "USR-OFF-01",
                        institutionId = "INST-SXA-2026",
                        username = "office_admin",
                        email = "documentation@stxaviers.edu",
                        password = "Password@123",
                        role = "OFFICE",
                        displayName = "Rajesh Verma (Documentation Officer)",
                        phoneNumber = "9833322110",
                        authPin = "1234",
                        isOtpVerified = true
                    )
                )
                dao.insertUsers(initialUsers)

                // 4. Pre-fed Faculty & Staff Members (Each with Unique Phone Number)
                val initialStaff = listOf(
                    StaffEntity(
                        id = "FAC-PRIN-01",
                        name = "Prof. Robert Vance, Ph.D.",
                        roleType = "PRINCIPAL",
                        designation = "Principal & Executive Director",
                        department = "Executive Directorate",
                        phone = "9811122334",
                        email = "principal@stxaviers.edu",
                        qualification = "Ph.D. Educational Leadership, M.Sc.",
                        experienceYears = 24,
                        govtRegNumber = "SXA-EXEC-001",
                        isPreAuthorizedByPrincipal = true,
                        isOtpVerified = true,
                        authPin = "9876",
                        isPresentToday = true,
                        checkInTime = "07:45 AM",
                        avatarColorHex = "#0B2545",
                        isClassIncharge = false,
                        idCardStatus = "APPROVED_BY_PRINCIPAL",
                        convertedDigitalIdNumber = "SXA-DIGI-PRIN-001"
                    ),
                    StaffEntity(
                        id = "FAC-TCH-01",
                        name = "Dr. Meenakshi Sundaram",
                        roleType = "TEACHING",
                        designation = "Senior Faculty in Physics & Science",
                        department = "Science Division",
                        phone = "9876543210",
                        email = "m.sundaram@stxaviers.edu",
                        qualification = "M.Sc., B.Ed., Ph.D. Physics",
                        experienceYears = 14,
                        govtRegNumber = "SXA-FAC-101",
                        isPreAuthorizedByPrincipal = true,
                        isOtpVerified = true,
                        authPin = "1234",
                        isPresentToday = true,
                        checkInTime = "07:55 AM",
                        avatarColorHex = "#15803D",
                        isClassIncharge = false,
                        assignedSubjects = "Physics, General Science",
                        idCardStatus = "APPROVED_BY_PRINCIPAL",
                        convertedDigitalIdNumber = "SXA-DIGI-FAC-101"
                    ),
                    StaffEntity(
                        id = "FAC-TCH-02",
                        name = "Mrs. Sunita Kapoor",
                        roleType = "INCHARGE",
                        designation = "Class In-Charge 10-A & Mathematics Faculty",
                        department = "Mathematical Sciences",
                        phone = "9876543211",
                        email = "sunita.kapoor@stxaviers.edu",
                        qualification = "M.Sc. Mathematics, B.Ed.",
                        experienceYears = 12,
                        govtRegNumber = "SXA-FAC-102",
                        isPreAuthorizedByPrincipal = true,
                        isOtpVerified = true,
                        authPin = "1234",
                        isPresentToday = true,
                        checkInTime = "08:00 AM",
                        avatarColorHex = "#E65100",
                        isClassIncharge = true,
                        inchargeClass = "10-A",
                        assignedSubjects = "Mathematics, Advanced Calculus",
                        idCardStatus = "APPROVED_BY_PRINCIPAL",
                        convertedDigitalIdNumber = "SXA-DIGI-FAC-102"
                    ),
                    StaffEntity(
                        id = "FAC-TCH-03",
                        name = "David K. Reynolds",
                        roleType = "TEACHING",
                        designation = "Faculty in English Literature",
                        department = "Languages & Humanities",
                        phone = "9833344556",
                        email = "david.reynolds@stxaviers.edu",
                        qualification = "M.A. English, B.Ed.",
                        experienceYears = 11,
                        govtRegNumber = "SXA-FAC-103",
                        isPreAuthorizedByPrincipal = true,
                        isOtpVerified = false,
                        authPin = "1234",
                        isPresentToday = true,
                        checkInTime = "08:05 AM",
                        avatarColorHex = "#0B2545",
                        isClassIncharge = false,
                        assignedSubjects = "English Literature",
                        idCardStatus = "APPROVED_BY_PRINCIPAL",
                        convertedDigitalIdNumber = "SXA-DIGI-FAC-103"
                    ),
                    StaffEntity(
                        id = "FAC-ACC-01",
                        name = "Suresh Mehta",
                        roleType = "ACCOUNTANT",
                        designation = "Bursar & Accounts Head",
                        department = "Finance & Accounts",
                        phone = "9844455667",
                        email = "accounts@stxaviers.edu",
                        qualification = "M.Com, Chartered Accountant (Inter)",
                        experienceYears = 16,
                        govtRegNumber = "SXA-ACC-001",
                        isPreAuthorizedByPrincipal = true,
                        isOtpVerified = true,
                        authPin = "1234",
                        isPresentToday = true,
                        checkInTime = "08:15 AM",
                        avatarColorHex = "#D97706",
                        isClassIncharge = false,
                        idCardStatus = "APPROVED_BY_PRINCIPAL",
                        convertedDigitalIdNumber = "SXA-DIGI-ACC-001"
                    ),
                    StaffEntity(
                        id = "FAC-OPS-01",
                        name = "Rajesh Verma",
                        roleType = "NON_TEACHING",
                        designation = "Campus Operations & Transport Head",
                        department = "Estate & Transport",
                        phone = "9855566778",
                        email = "operations@stxaviers.edu",
                        qualification = "B.Com, Campus Logistics",
                        experienceYears = 18,
                        govtRegNumber = "SXA-OPS-001",
                        isPreAuthorizedByPrincipal = true,
                        isOtpVerified = true,
                        authPin = "1234",
                        isPresentToday = true,
                        checkInTime = "07:15 AM",
                        avatarColorHex = "#15803D",
                        isClassIncharge = false,
                        idCardStatus = "APPROVED_BY_PRINCIPAL",
                        convertedDigitalIdNumber = "SXA-DIGI-OPS-001"
                    )
                )
                dao.insertStaffList(initialStaff)

                // 5. Pre-fed Students with Admission Numbers & Privacy Boundaries
                val initialStudents = listOf(
                    StudentEntity(
                        id = "ADM-2026-001",
                        admissionNumber = "ADM-2026-001",
                        name = "Aarav Sharma",
                        grade = "10",
                        section = "A",
                        rollNumber = 1,
                        gender = "Male",
                        dateOfBirth = "2011-03-15",
                        guardianName = "Vikram Sharma",
                        guardianPhone = "+91 91234 56780",
                        studentPhone = "9123456780",
                        email = "aarav.sharma@stxaviers.edu",
                        bloodGroup = "O+",
                        attendancePercentage = 94.5f,
                        feeDue = 0.0,
                        feePaid = 65000.0,
                        avatarColorHex = "#0B2545",
                        address = "42 North Avenue, Estate Square",
                        realIdPhotoUri = "asset://aarav_sharma.jpg",
                        convertedDigitalId = "SXA-DIGI-STU-001",
                        digitalIdStatus = "APPROVED_BY_PRINCIPAL"
                    ),
                    StudentEntity(
                        id = "ADM-2026-002",
                        admissionNumber = "ADM-2026-002",
                        name = "Ananya Iyer",
                        grade = "10",
                        section = "A",
                        rollNumber = 2,
                        gender = "Female",
                        dateOfBirth = "2011-06-21",
                        guardianName = "Suresh Iyer",
                        guardianPhone = "+91 91234 56781",
                        studentPhone = "9123456781",
                        email = "ananya.iyer@stxaviers.edu",
                        bloodGroup = "B+",
                        attendancePercentage = 97.2f,
                        feeDue = 0.0,
                        feePaid = 65000.0,
                        avatarColorHex = "#15803D",
                        realIdPhotoUri = "asset://ananya_iyer.jpg",
                        convertedDigitalId = "SXA-DIGI-STU-002",
                        digitalIdStatus = "APPROVED_BY_PRINCIPAL"
                    ),
                    StudentEntity(
                        id = "ADM-2026-003",
                        admissionNumber = "ADM-2026-003",
                        name = "Diya Sengupta",
                        grade = "10",
                        section = "A",
                        rollNumber = 3,
                        gender = "Female",
                        dateOfBirth = "2011-09-08",
                        guardianName = "Arun Sengupta",
                        guardianPhone = "+91 91234 56782",
                        studentPhone = "9123456782",
                        email = "diya.s@stxaviers.edu",
                        bloodGroup = "AB+",
                        attendancePercentage = 92.0f,
                        feeDue = 0.0,
                        feePaid = 65000.0,
                        avatarColorHex = "#0B2545",
                        realIdPhotoUri = "asset://diya_sengupta.jpg",
                        convertedDigitalId = "SXA-DIGI-STU-003",
                        digitalIdStatus = "APPROVED_BY_PRINCIPAL"
                    ),
                    StudentEntity(
                        id = "ADM-2026-004",
                        admissionNumber = "ADM-2026-004",
                        name = "Kabir Patel",
                        grade = "10",
                        section = "A",
                        rollNumber = 4,
                        gender = "Male",
                        dateOfBirth = "2011-01-27",
                        guardianName = "Nilesh Patel",
                        guardianPhone = "+91 91234 56783",
                        studentPhone = "9123456783",
                        email = "kabir.patel@stxaviers.edu",
                        bloodGroup = "A+",
                        attendancePercentage = 82.4f,
                        feeDue = 15000.0,
                        feePaid = 50000.0,
                        avatarColorHex = "#E65100",
                        realIdPhotoUri = null,
                        convertedDigitalId = "SXA-DIGI-STU-004",
                        digitalIdStatus = "PENDING_TEACHER_CAPTURE"
                    ),
                    StudentEntity(
                        id = "ADM-2026-005",
                        admissionNumber = "ADM-2026-005",
                        name = "Rohan Malhotra",
                        grade = "10",
                        section = "A",
                        rollNumber = 5,
                        gender = "Male",
                        dateOfBirth = "2011-11-14",
                        guardianName = "Rakesh Malhotra",
                        guardianPhone = "+91 91234 56784",
                        studentPhone = "9123456784",
                        email = "rohan.m@stxaviers.edu",
                        bloodGroup = "O-",
                        attendancePercentage = 88.5f,
                        feeDue = 5000.0,
                        feePaid = 60000.0,
                        avatarColorHex = "#15803D",
                        realIdPhotoUri = null,
                        convertedDigitalId = "SXA-DIGI-STU-005",
                        digitalIdStatus = "PENDING_TEACHER_CAPTURE"
                    )
                )
                dao.insertStudents(initialStudents)

                // 6. Pre-fed Examination Records (Report Card Generator)
                val initialExams = listOf(
                    ExamRecordEntity(
                        examId = "EXAM-10A-MATH-001",
                        examType = "SUMMATIVE_ASSESSMENT",
                        examName = "Term 1 Summative Assessment 2026",
                        classSection = "10-A",
                        subject = "Mathematics",
                        studentId = "ADM-2026-001",
                        studentName = "Aarav Sharma",
                        rollNumber = 1,
                        marksObtained = 95f,
                        maxMarks = 100f,
                        grade = "A+",
                        teacherRemarks = "Outstanding conceptual clarity in Algebra and Geometry."
                    ),
                    ExamRecordEntity(
                        examId = "EXAM-10A-PHY-001",
                        examType = "SUMMATIVE_ASSESSMENT",
                        examName = "Term 1 Summative Assessment 2026",
                        classSection = "10-A",
                        subject = "Physics",
                        studentId = "ADM-2026-001",
                        studentName = "Aarav Sharma",
                        rollNumber = 1,
                        marksObtained = 92f,
                        maxMarks = 100f,
                        grade = "A+",
                        teacherRemarks = "Exceptional analytical reasoning and practical experiment skills."
                    ),
                    ExamRecordEntity(
                        examId = "EXAM-10A-ENG-001",
                        examType = "SUMMATIVE_ASSESSMENT",
                        examName = "Term 1 Summative Assessment 2026",
                        classSection = "10-A",
                        subject = "English Literature",
                        studentId = "ADM-2026-001",
                        studentName = "Aarav Sharma",
                        rollNumber = 1,
                        marksObtained = 88f,
                        maxMarks = 100f,
                        grade = "A",
                        teacherRemarks = "Articulate textual commentary and good prose structure."
                    ),
                    ExamRecordEntity(
                        examId = "EXAM-10A-CHEM-001",
                        examType = "SUMMATIVE_ASSESSMENT",
                        examName = "Term 1 Summative Assessment 2026",
                        classSection = "10-A",
                        subject = "Chemistry",
                        studentId = "ADM-2026-001",
                        studentName = "Aarav Sharma",
                        rollNumber = 1,
                        marksObtained = 94f,
                        maxMarks = 100f,
                        grade = "A+",
                        teacherRemarks = "Thorough understanding of Chemical Bonding and Stoichiometry."
                    ),
                    ExamRecordEntity(
                        examId = "EXAM-10A-MATH-004",
                        examType = "SUMMATIVE_ASSESSMENT",
                        examName = "Term 1 Summative Assessment 2026",
                        classSection = "10-A",
                        subject = "Mathematics",
                        studentId = "ADM-2026-004",
                        studentName = "Kabir Patel",
                        rollNumber = 4,
                        marksObtained = 76f,
                        maxMarks = 100f,
                        grade = "B+",
                        teacherRemarks = "Good effort; needs more practice in Trigonometry."
                    )
                )
                dao.insertExamRecords(initialExams)

                // 7. Pre-fed Fee Records (Amounts in Paise/Cents with Digital Receipts)
                val initialFees = listOf(
                    FeeRecordEntity(
                        id = "FEE-2026-001",
                        studentId = "ADM-2026-001",
                        admissionNumber = "ADM-2026-001",
                        studentName = "Aarav Sharma",
                        gradeSection = "Grade 10-A",
                        term = "Term 1 (Academic Year 2026-27)",
                        feeType = "Composite Tuition & Advanced Science Lab",
                        totalAmount = 6500000L, //  65,000.00
                        paidAmount = 6500000L,
                        dueAmount = 0L,
                        status = "PAID",
                        receiptNumber = "SXA-REC-2026-9901",
                        paidDate = "2026-08-05",
                        paymentMode = "ONLINE",
                        verifiedByIncharge = true
                    ),
                    FeeRecordEntity(
                        id = "FEE-2026-002",
                        studentId = "ADM-2026-002",
                        admissionNumber = "ADM-2026-002",
                        studentName = "Ananya Iyer",
                        gradeSection = "Grade 10-A",
                        term = "Term 1 (Academic Year 2026-27)",
                        feeType = "Composite Tuition & Robotics Laboratory",
                        totalAmount = 6500000L,
                        paidAmount = 6500000L,
                        dueAmount = 0L,
                        status = "PAID",
                        receiptNumber = "SXA-REC-2026-9902",
                        paidDate = "2026-08-04",
                        paymentMode = "ONLINE",
                        verifiedByIncharge = true
                    ),
                    FeeRecordEntity(
                        id = "FEE-2026-003",
                        studentId = "ADM-2026-004",
                        admissionNumber = "ADM-2026-004",
                        studentName = "Kabir Patel",
                        gradeSection = "Grade 10-A",
                        term = "Term 1 (Academic Year 2026-27)",
                        feeType = "Composite Tuition & Transport Installment 1",
                        totalAmount = 6500000L,
                        paidAmount = 5000000L,
                        dueAmount = 1500000L,
                        status = "PARTIAL",
                        receiptNumber = "SXA-REC-2026-9903",
                        paidDate = "2026-08-10",
                        paymentMode = "OFFLINE_ENTRY",
                        verifiedByIncharge = true
                    )
                )
                dao.insertFeeRecords(initialFees)

                // 8. Pre-fed Timetable Slots (Class 10-A)
                val initialTimetable = listOf(
                    TimetableSlotEntity(
                        slotId = "TT-10A-MON-1",
                        classSection = "10-A",
                        dayOfWeek = "Monday",
                        periodNumber = 1,
                        timeRange = "08:30 - 09:15 AM",
                        subject = "Mathematics",
                        teacherName = "Mrs. Sunita Kapoor",
                        teacherId = "FAC-TCH-02",
                        roomNumber = "Room 304"
                    ),
                    TimetableSlotEntity(
                        slotId = "TT-10A-MON-2",
                        classSection = "10-A",
                        dayOfWeek = "Monday",
                        periodNumber = 2,
                        timeRange = "09:15 - 10:00 AM",
                        subject = "Physics (Science)",
                        teacherName = "Dr. Meenakshi Sundaram",
                        teacherId = "FAC-TCH-01",
                        roomNumber = "Physics Lab A"
                    ),
                    TimetableSlotEntity(
                        slotId = "TT-10A-MON-3",
                        classSection = "10-A",
                        dayOfWeek = "Monday",
                        periodNumber = 3,
                        timeRange = "10:00 - 10:45 AM",
                        subject = "English Literature",
                        teacherName = "David K. Reynolds",
                        teacherId = "FAC-TCH-03",
                        roomNumber = "Room 304"
                    ),
                    TimetableSlotEntity(
                        slotId = "TT-10A-MON-4",
                        classSection = "10-A",
                        dayOfWeek = "Monday",
                        periodNumber = 4,
                        timeRange = "11:15 - 12:00 PM",
                        subject = "Chemistry",
                        teacherName = "Dr. Meenakshi Sundaram",
                        teacherId = "FAC-TCH-01",
                        roomNumber = "Chemistry Lab"
                    )
                )
                dao.insertTimetableSlots(initialTimetable)

                // 9. Pre-fed Admission Applications
                val initialAdmissions = listOf(
                    AdmissionApplicationEntity(
                        applicationId = "ADM-APP-2026-081",
                        studentName = "Zara Qureshi",
                        dob = "2015-05-18",
                        gender = "Female",
                        requestedClass = "Class 5",
                        guardianName = "Farhan Qureshi",
                        guardianPhone = "+91 98777 88990",
                        guardianEmail = "f.qureshi@gmail.com",
                        address = "Green Park Extension, House 12",
                        previousSchool = "Springdales Preparatory",
                        status = "PENDING_VERIFICATION",
                        applicationDate = "2026-09-14",
                        remarks = "Birth certificate and grade 4 report card verified."
                    ),
                    AdmissionApplicationEntity(
                        applicationId = "ADM-APP-2026-082",
                        studentName = "Devansh Sharma",
                        dob = "2018-09-12",
                        gender = "Male",
                        requestedClass = "Class 2",
                        guardianName = "Pooja Sharma",
                        guardianPhone = "+91 98666 77889",
                        guardianEmail = "pooja.s@gmail.com",
                        address = "Model Town, Sector 3",
                        previousSchool = "Little Wonders Nursery",
                        status = "VERIFIED_OFFICE",
                        applicationDate = "2026-09-12",
                        remarks = "Documents verified. Awaiting Principal final approval."
                    )
                )
                initialAdmissions.forEach { dao.insertAdmissionApplication(it) }

                // 10. Pre-fed Institutional Bills & Government Compliance
                val initialBills = listOf(
                    InstitutionalBillEntity(
                        billId = "BILL-ELEC-2026-08",
                        billType = "ELECTRICITY",
                        vendorName = "State Electricity Distribution Corp",
                        billNumber = "DISCOM-2026-8819",
                        billingMonth = "August 2026",
                        amountRupees = 148500L,
                        dueDate = "2026-09-20",
                        paidDate = "2026-09-10",
                        paymentStatus = "PAID",
                        receiptRef = "RCP-DISCOM-99012"
                    ),
                    InstitutionalBillEntity(
                        billId = "BILL-NET-2026-09",
                        billType = "INTERNET",
                        vendorName = "Fiber High-Speed Telecom Ltd",
                        billNumber = "FBR-ACAD-2026-09",
                        billingMonth = "September 2026",
                        amountRupees = 18500L,
                        dueDate = "2026-09-25",
                        paidDate = null,
                        paymentStatus = "PENDING",
                        receiptRef = "PENDING_INVOICE"
                    )
                )
                dao.insertBills(initialBills)

                val initialCompliance = listOf(
                    GovernmentComplianceEntity(
                        complianceId = "COMPL-FIRE-2026",
                        documentTitle = "State Fire Safety Clearance Certificate",
                        authorityName = "Municipal Fire & Rescue Services",
                        registrationNumber = "NOC-FIRE-2026-7781",
                        issueDate = "2026-01-15",
                        expiryDate = "2027-01-14",
                        renewalReminderDays = 45,
                        status = "ACTIVE",
                        responsibleStaff = "Rajesh Verma (Operations Head)"
                    ),
                    GovernmentComplianceEntity(
                        complianceId = "COMPL-BOARD-2026",
                        documentTitle = "Secondary School Board Affiliation License",
                        authorityName = "National Examination Board Directorate",
                        registrationNumber = "AFFIL-CBSE-1130492-R",
                        issueDate = "2024-04-01",
                        expiryDate = "2029-03-31",
                        renewalReminderDays = 90,
                        status = "ACTIVE",
                        responsibleStaff = "Prof. Robert Vance (Principal)"
                    )
                )
                dao.insertComplianceDocuments(initialCompliance)

                // 11. Pre-fed Certificates, ID Cards, Diary, Notices, Audit Logs
                val initialCertificates = listOf(
                    CertificateEntity(
                        certificateId = "CERT-BON-2026-089",
                        certificateType = "BONAFIDE",
                        studentName = "Aarav Sharma",
                        studentId = "ADM-2026-001",
                        grade = "Grade 10-A",
                        issueDate = "2026-09-10",
                        academicYear = "2026-2027",
                        purposeOrAchievement = "National Science Olympiad & Passport Verification",
                        verifiedBy = "Prof. Robert Vance (Principal)",
                        digitalBarcode = "SXA-CERT-9942-VERIFIED",
                        applicationStatus = "APPROVED_BY_PRINCIPAL",
                        appliedBy = "Student - Aarav Sharma",
                        appliedDate = "2026-09-08",
                        verifiedByIncharge = "Mrs. Sunita Kapoor (Class Incharge)",
                        approvedByPrincipal = "Prof. Robert Vance (Principal)",
                        inchargeRemarks = "Attendance 94.5%, Conduct certified exemplary",
                        principalRemarks = "Approved under Executive Master Seal",
                        isDownloadUnlocked = true,
                        secureWhatsAppToken = "WHT-SXA-CERT-7731"
                    )
                )
                initialCertificates.forEach { dao.insertCertificate(it) }

                val initialIdCards = listOf(
                    IdCardRecordEntity(
                        cardId = "IDC-PRIN-001",
                        personId = "FAC-PRIN-01",
                        personName = "Prof. Robert Vance, Ph.D.",
                        roleCategory = "PRINCIPAL",
                        designationOrGrade = "Principal & Executive Director",
                        department = "Executive Directorate",
                        phone = "+91 98111 22334",
                        bloodGroup = "A+",
                        convertedDigitalCardId = "SXA-DIGI-PRIN-001",
                        barcodeNumber = "SXA-DIR-001-BARCODE-0001",
                        issueDate = "2026-08-01",
                        verificationStatus = "APPROVED_BY_PRINCIPAL",
                        inchargeRemarks = "Board of Trustees Clearance",
                        principalRemarks = "Executive Master Seal",
                        approvedBy = "Board of Governors",
                        securitySealNumber = "SXA-SEAL-0001"
                    ),
                    IdCardRecordEntity(
                        cardId = "IDC-STU-001",
                        personId = "ADM-2026-001",
                        personName = "Aarav Sharma",
                        roleCategory = "STUDENT",
                        designationOrGrade = "Grade 10-A (Roll #1)",
                        department = "Secondary Academic Division",
                        phone = "+91 91234 56780",
                        bloodGroup = "O+",
                        convertedDigitalCardId = "SXA-DIGI-STU-001",
                        barcodeNumber = "SXA-STU-001-BARCODE-1024",
                        issueDate = "2026-08-15",
                        verificationStatus = "APPROVED_BY_PRINCIPAL",
                        inchargeRemarks = "Grade 10-A enrollment verified",
                        principalRemarks = "Official Scholar Pass Validated",
                        approvedBy = "Prof. Robert Vance (Principal)",
                        securitySealNumber = "SXA-SEAL-1024"
                    )
                )
                initialIdCards.forEach { dao.insertIdCard(it) }

                val initialDiary = listOf(
                    SchoolDiaryEntity(
                        id = "DIARY-2026-001",
                        date = "2026-09-15",
                        classSection = "Grade 10-A",
                        subject = "Mathematics",
                        topicCovered = "Chapter 4: Quadratic Equations - Formula & Discriminant Analysis",
                        homeworkAssigned = "Complete Exercise 4.3 (Problems 1 through 10) in school notebook.",
                        specialInstructions = "Bring geometric instruments and standard log tables tomorrow.",
                        teacherName = "Mrs. Sunita Kapoor",
                        inchargeVerified = true
                    ),
                    SchoolDiaryEntity(
                        id = "DIARY-2026-002",
                        date = "2026-09-15",
                        classSection = "Grade 10-A",
                        subject = "Physics (Science)",
                        topicCovered = "Optics & Light: Refraction through Spherical Lenses",
                        homeworkAssigned = "Draw standard ray diagrams for convex and concave lenses in lab record.",
                        specialInstructions = "Physics laboratory coats are mandatory for the afternoon practical.",
                        teacherName = "Dr. Meenakshi Sundaram",
                        inchargeVerified = true
                    )
                )
                dao.insertDiaryEntries(initialDiary)

                val initialNotices = listOf(
                    NoticeMeetingEntity(
                        id = "NOT-001",
                        title = "Annual Inter-Academy Science & Technology Conclave",
                        type = "CIRCULAR",
                        date = "2026-09-18",
                        time = "09:30 AM",
                        venueOrAudience = "ALL",
                        description = "All students of Grades 9 through 12 are invited to register science projects. Keynote address by visiting fellows.",
                        isUrgent = false,
                        organizer = "Office of the Principal, St. Xavier's International Academy"
                    ),
                    NoticeMeetingEntity(
                        id = "NOT-002",
                        title = "Term 1 Composite Fee Remittance Reminder",
                        type = "CIRCULAR",
                        date = "2026-09-12",
                        time = "Urgent Notice",
                        venueOrAudience = "PARENTS",
                        description = "Parents are requested to clear pending Term 1 dues through the digital portal. Official receipts are issued instantly.",
                        isUrgent = true,
                        organizer = "Bursar & Accounts Directorate"
                    )
                )
                dao.insertNotices(initialNotices)

                // 12. Non-Repudiable Institutional Audit Logs
                val initialAuditLogs = listOf(
                    AuditLogEntity(
                        logId = 1,
                        timestamp = System.currentTimeMillis() - 86400000,
                        formattedTime = "2026-09-14T08:15:00Z",
                        action = "FACULTY_SUBMIT_ATTENDANCE_BATCH",
                        entityType = "ATTENDANCE_BATCH",
                        entityId = "BATCH-OCR-10A-9921",
                        actorUserId = "FAC-TCH-01",
                        actorName = "Dr. Meenakshi Sundaram",
                        actorRole = "ROLE_TEACHER",
                        previousState = "REGISTER_CAMERA_SCANNED",
                        newState = "COMMITTED_CENTRAL",
                        remarks = "Morning Assembly OCR scan submitted for Section 10-A. 4 Present, 1 Absent."
                    ),
                    AuditLogEntity(
                        logId = 2,
                        timestamp = System.currentTimeMillis() - 82800000,
                        formattedTime = "2026-09-14T09:20:00Z",
                        action = "INCHARGE_VERIFY_ATTENDANCE_BATCH",
                        entityType = "ATTENDANCE_BATCH",
                        entityId = "BATCH-OCR-10A-9921",
                        actorUserId = "FAC-TCH-02",
                        actorName = "Mrs. Sunita Kapoor",
                        actorRole = "ROLE_INCHARGE",
                        previousState = "PENDING_INCHARGE_VERIFICATION",
                        newState = "VERIFIED_BY_INCHARGE",
                        remarks = "Section Incharge cross-verified medical slip for absent scholar Kabir Patel."
                    ),
                    AuditLogEntity(
                        logId = 3,
                        timestamp = System.currentTimeMillis() - 79200000,
                        formattedTime = "2026-09-14T10:30:00Z",
                        action = "PRINCIPAL_APPROVE_ATTENDANCE_BATCH",
                        entityType = "ATTENDANCE_BATCH",
                        entityId = "BATCH-OCR-10A-9921",
                        actorUserId = "FAC-PRIN-01",
                        actorName = "Prof. Robert Vance, Ph.D.",
                        actorRole = "ROLE_PRINCIPAL",
                        previousState = "VERIFIED_BY_INCHARGE",
                        newState = "PERMANENT_SEAL_COMMITTED",
                        remarks = "Executive Seal attached to Monthly Central Attendance Ledger."
                    )
                )
                dao.insertAuditLogs(initialAuditLogs)
            }
        }
    }
}
