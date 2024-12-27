function feasibles = compareNumGWs(implicit)
    if (implicit)
        str = 'implicit';
    else
        str = 'restricted';
    end
    
    if (implicit)
        file = 'D:\pdv\workspace\realtime-tdma\log\redo\numGWs\implicit\convert.txt';
    else
        file = 'D:\pdv\workspace\realtime-tdma\log\redo\numGWs\restricted\convert.txt';
    end
    
    data = extract_data(file);
    gwsCol = data(:, 8);
    feasibles = [];
    for gws = 2:2:6
        data1 = data(find(gwsCol==gws), :);
        arow = zeros(1, 5);
        for i=0:4
            ch = 2^i;
            data2 = data1(find(data1(:, 6)==ch), :);
            arow(i+1) = sum(data2(:, 7)/size(data2, 1)); 
        end
        feasibles = [feasibles; arow];
    end
    
    descs = {'1', '2', '4', '8', '16'};
    bar(feasibles');
    set(gca,'XTickLabel', descs);
    title(str);
    ylabel('schedulability rate (%)');
    legend('2 GWs', '4 GWs', '6 GWs');
    title(str);
    xlabel('# of channels');
    utils = data(:, 5);
    channels = data(:, 6);
    scheds = data(:, 7);
    numGWs = data(:, 8);
    figure;
    hold on;
    norms = utils./channels;
    ids = find(scheds == 1);
    feasibles = norms(ids);
    cdfplot(feasibles);
    ids = find(scheds == 0);
    unfeasibles = norms(ids);
    %cdfplot(unfeasibles);
    grid on;
end

function data = extract_data(file)
    x = importdata(file);
    data = x.data;
end
