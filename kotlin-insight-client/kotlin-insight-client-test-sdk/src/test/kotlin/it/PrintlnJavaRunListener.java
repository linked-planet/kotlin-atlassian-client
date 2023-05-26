package it;

import org.junit.runner.Description;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;

public class PrintlnJavaRunListener extends RunListener {
    @Override
    public void testRunStarted(Description description) throws Exception {
        System.out.println(description.getDisplayName());
    }

    @Override
    public void testRunFinished(Result result) throws Exception {
        System.out.println("finished with success:" + result.wasSuccessful());
    }

    @Override
    public void testFailure(Failure failure) throws Exception {
        System.out.println(failure.getDescription().getDisplayName());
    }

    @Override
    public void testIgnored(Description description) throws Exception {
        System.out.println(description.getDisplayName());
    }

}
