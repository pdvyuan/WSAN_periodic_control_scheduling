function schedulabilityCheck
     for implicit = 1:-1:0
         for physical = 0:1
             checkACase(implicit, physical);
         end
     end
end


function checkACase(implicit, physical)
    % take LLF as example
    if (implicit)
        str = 'implicit deadline';
    else
        str = 'restricted deadline';
    end

    if (physical)
        str = [str ', realistic model'];
    else
        str = [str ', disk model'];
    end

    file = 'D:\pdv\workspace\realtime-tdma\log\redo\algorithms\';
    if (implicit)
        file = [file, 'implicit\'];
    else
        file = [file, 'restricted\'];
    end
    if (physical)
        file = [file, 'physical\'];
    else
        file = [file, 'random\'];
    end
    file = [file, 'convert.txt'];

    x = importdata(file);
    data = x.data;
    x = data(:, 47);
    fprintf('%s:', str);
    nFeasible = length(find(x == 1));
    nUnschedulable = length(find(x == 0));
    nCheckFail = length(find(x == -1));
    fprintf('feasible = %d, unschedulable = %d, check-failure = %d\n', nFeasible, nUnschedulable, nCheckFail);
    fprintf('recall = %.1f, false positive rate = %.1f\n', nCheckFail*100/(nCheckFail+nUnschedulable), nUnschedulable*100/(nUnschedulable+nFeasible));
    fprintf('\n');
end


