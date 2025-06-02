package ws.siri.yarnwrap.common;

public interface ScriptRuntime {
    public Object evaluate(String expr, String label) throws Exception;

    default public Object evaluate(String expr) throws Exception {
        return evaluate(expr, "<unnamed script>");
    }
}