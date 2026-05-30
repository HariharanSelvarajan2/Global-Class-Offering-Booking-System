$courseBaseUrl = "http://localhost:8081"
$bookingBaseUrl = "http://localhost:8082"

$samples = @(
    @{
        teacherId = "11111111-1111-4111-8111-111111111111"
        parentId = "21111111-1111-4111-8111-111111111111"
        timezone = "Asia/Kolkata"
        courseName = "Minecraft Coding"
        offeringName = "Kolkata Saturday Batch"
        localStart = "2026-06-06T18:00:00"
        localEnd = "2026-06-06T19:00:00"
    },
    @{
        teacherId = "12222222-2222-4222-8222-222222222222"
        parentId = "22222222-2222-4222-8222-222222222222"
        timezone = "America/New_York"
        courseName = "Python Coding"
        offeringName = "New York Morning Batch"
        localStart = "2026-06-07T09:00:00"
        localEnd = "2026-06-07T10:00:00"
    },
    @{
        teacherId = "13333333-3333-4333-8333-333333333333"
        parentId = "23333333-3333-4333-8333-333333333333"
        timezone = "Europe/London"
        courseName = "Robotics Basics"
        offeringName = "London Weekend Batch"
        localStart = "2026-06-08T16:00:00"
        localEnd = "2026-06-08T17:00:00"
    },
    @{
        teacherId = "14444444-4444-4444-8444-444444444444"
        parentId = "24444444-4444-4444-8444-444444444444"
        timezone = "Asia/Tokyo"
        courseName = "Game Design"
        offeringName = "Tokyo Evening Batch"
        localStart = "2026-06-09T19:00:00"
        localEnd = "2026-06-09T20:00:00"
    },
    @{
        teacherId = "15555555-5555-4555-8555-555555555555"
        parentId = "25555555-5555-4555-8555-555555555555"
        timezone = "Australia/Sydney"
        courseName = "Creative Coding"
        offeringName = "Sydney After School Batch"
        localStart = "2026-06-10T17:30:00"
        localEnd = "2026-06-10T18:30:00"
    },
    @{
        teacherId = "16666666-6666-4666-8666-666666666666"
        parentId = "26666666-6666-4666-8666-666666666666"
        timezone = "Europe/Paris"
        courseName = "Digital Art"
        offeringName = "Paris Studio Batch"
        localStart = "2026-06-11T18:00:00"
        localEnd = "2026-06-11T19:00:00"
    },
    @{
        teacherId = "17777777-7777-4777-8777-777777777777"
        parentId = "27777777-7777-4777-8777-777777777777"
        timezone = "America/Los_Angeles"
        courseName = "Web Design"
        offeringName = "Los Angeles Beginner Batch"
        localStart = "2026-06-12T15:30:00"
        localEnd = "2026-06-12T16:30:00"
    },
    @{
        teacherId = "18888888-8888-4888-8888-888888888888"
        parentId = "28888888-8888-4888-8888-888888888888"
        timezone = "Asia/Dubai"
        courseName = "AI For Kids"
        offeringName = "Dubai Innovation Batch"
        localStart = "2026-06-13T18:30:00"
        localEnd = "2026-06-13T19:30:00"
    },
    @{
        teacherId = "19999999-9999-4999-8999-999999999999"
        parentId = "29999999-9999-4999-8999-999999999999"
        timezone = "Africa/Johannesburg"
        courseName = "Math Through Games"
        offeringName = "Johannesburg Logic Batch"
        localStart = "2026-06-14T10:00:00"
        localEnd = "2026-06-14T11:00:00"
    },
    @{
        teacherId = "1aaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa"
        parentId = "2aaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa"
        timezone = "Pacific/Auckland"
        courseName = "App Building"
        offeringName = "Auckland Creator Batch"
        localStart = "2026-06-15T16:00:00"
        localEnd = "2026-06-15T17:00:00"
    }
)

$created = @()

foreach ($sample in $samples) {
    $offeringBody = @{
        teacherId = $sample.teacherId
        courseName = $sample.courseName
        offeringName = $sample.offeringName
        teacherTimezone = $sample.timezone
    } | ConvertTo-Json

    $offering = Invoke-RestMethod -Method Post `
        -Uri "$courseBaseUrl/api/v1/teacher/offerings" `
        -ContentType "application/json" `
        -Body $offeringBody

    $sessionBody = @{
        localStart = $sample.localStart
        localEnd = $sample.localEnd
        timezone = $sample.timezone
    } | ConvertTo-Json

    $session = Invoke-RestMethod -Method Post `
        -Uri "$courseBaseUrl/api/v1/teacher/offerings/$($offering.id)/sessions" `
        -ContentType "application/json" `
        -Body $sessionBody

    $bookingBody = @{
        parentId = $sample.parentId
        offeringId = $offering.id
        timezone = $sample.timezone
    } | ConvertTo-Json

    $booking = Invoke-RestMethod -Method Post `
        -Uri "$bookingBaseUrl/api/v1/parent/bookings" `
        -ContentType "application/json" `
        -Body $bookingBody

    $created += [pscustomobject]@{
        teacherId = $sample.teacherId
        parentId = $sample.parentId
        timezone = $sample.timezone
        offeringId = $offering.id
        sessionId = $session.id
        bookingId = $booking.id
    }
}

$created | Format-Table -AutoSize
