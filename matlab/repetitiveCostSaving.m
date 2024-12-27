function repetitiveCostSaving(physical)
    group = [];
    saveRate = [];
    
    for implicit = 1:-1:0
        if (implicit == 1)
            s1 = 'imp';
        else
            s1 = 'res';
        end
        for piggyback = 0:1
            if (piggyback == 1)
                s2 = 'aggr   ';
            else
                s2 = 'no aggr';
            end
            [f, s] = compareRepSch(implicit, physical, piggyback, 0);
            saveRate = [saveRate; s];
            text = [s1, '/', s2];
            group = [group; repmat(text, size(s, 1), size(s, 2))];
        end
    end
    figure;
    boxplot(saveRate, group);
    ylabel('cost saving');
    if (physical)
        title('realistic model');
    else
        title('disk model');
    end
    
end