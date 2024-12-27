function outputRuntimeTableOA
    fprintf('\\begin{table}\n');
    fprintf('\\centering\n');
    fprintf('\\begin{tabular} { c | c | c | c | c } \n');
    fprintf(' & imp./disk & imp./real. & res./disk & res./real. \\\\ \\hline\n');
    ts = [];
    for implicit = 1:-1:0
        for physical = 0:1
            [f, o, t] = compareOA(implicit, physical, 0);
            ts = [ts; t];
        end
    end
    for i=1:2
        if (i == 1)
            fprintf('w/o oa');
        else
            fprintf('w. oa');
        end
        for j=1:size(ts, 1)
            fprintf(' & %.1f ', ts(j, i));
        end
        if (i == 1)
            fprintf('\\\\ \\hline');
        end
        fprintf('\n');
    end
    fprintf('\\end{tabular}\n');
    fprintf('\\end{table}\n');
end