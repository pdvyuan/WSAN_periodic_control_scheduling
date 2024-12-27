function outputRuntimeTable
    fprintf('\\begin{table}\n');
    fprintf('\\centering\n');
    fprintf('\\begin{tabular} {c || c | c | c |c | c |c | c |c | c } \n');
    fprintf('algorithm & RM & DM & PDM & CLLF & EDF & LLF & EPD & EDZL & ALICE & TASA & RANDOM \\\\ \\hline\n');
    for implicit = 1:-1:0
        for physical = 1:1
            [u, f, t, stdt, m, p] = plotSchedulability(implicit, physical, 0);
            if (implicit)
                fprintf('imp./');
            else
                fprintf('res./');
            end
            if (physical)
                fprintf('real.');
            else
                fprintf('disk');
            end
            for i=1:length(t)
                fprintf(' & $%.1f$ ', t(i));
            end
            if (implicit == 0 && physical == 1)
                fprintf('\n');
            else
                fprintf('\\\\ \\hline\n');
            end
        end
    end
    fprintf('\\end{tabular}\n');
    fprintf('\\end{table}\n');
end