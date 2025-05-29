package reporting;


import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.ExtentTest;
import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestResult;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;

public class ExtentReportListener implements ITestListener {
    public static ExtentReports extentReports;
    public static ThreadLocal<ExtentTest> extentTest = new ThreadLocal<>();
    public static ExtentTest getTest() {
        return extentTest.get();
    }
    public void onStart(ITestContext context) {
        String fileName = ExtentReportManager.getReportNameWithTimeStamp();
        String fullReportPath = System.getProperty("user.dir") + "/src/main/report/" + fileName;
        extentReports = ExtentReportManager.createInstance(fullReportPath, "Test API Automation Report", "Test ExecutionReport");
    }

    public void onFinish(ITestContext context) {
        if (extentReports != null)
            extentReports.flush();
    }

    public void onTestStart(ITestResult result) {
        String className = result.getTestClass().getRealClass().getSimpleName();
        String methodName = result.getMethod().getMethodName();

        ExtentTest test = extentReports.createTest("Test Name: " + className + " - " + methodName,
                result.getMethod().getDescription());
        extentTest.set(test);
    }

    public void onTestFailure(ITestResult result) {
        String msg = result.getThrowable().getMessage();
        if (msg != null && !msg.isEmpty()) {
            // Escape HTML characters first
            msg = msg.replace("&", "&amp;")
                    .replace("<", "&lt;")
                    .replace(">", "&gt;");

            // Then split and insert <br>
            String[] words = msg.split("\\s+");
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < words.length; i++)
                sb.append(words[i]).append(" ").append((i + 1) % 5 == 0 ? "<br>" : "");

            // Output to HTML report
          //  System.out.println("Error (HTML):<br>" + sb.toString());
            String htmlMsg = "<div style='text-align:left; font-size:12px;'>" + sb.toString() + "</div>";
            ExtentReportManager.logFailureDetails(htmlMsg);
        }
      //  ExtentReportManager.logFailureDetails(result.getThrowable().getMessage());
        String stackTrace = Arrays.toString(result.getThrowable().getStackTrace());
        stackTrace = stackTrace.replaceAll(",", "<br>");
        String formmatedTrace = "<details>\n" +
                "    <summary>Click Here To See Exception Logs</summary>\n" +
                "    " + stackTrace + "\n" +
                "</details>\n";
        ExtentReportManager.logExceptionDetails(formmatedTrace);
    }
}
