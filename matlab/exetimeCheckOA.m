function exetimeCheckOA
    group = [];
    times = [];
    
    fig = figure;
    set(gca, 'FontSize', 15);
    hold on;
    xlabel('transmissions in a hyper-period');
    ylabel('execution time (ms)');
    
%     t = checkACaseOfExetimeOA(1, 0, fig);
%     times = [times; t];
%     group = [group; repmat('imp/disk', size(t))];
    
    t = checkACaseOfExetimeOA(1, 1, fig);
    times = [times; t];
    group = [group; repmat('implicit  ', size(t))];
    
%     t = checkACaseOfExetimeOA(0, 0, fig);
%     times = [times; t];
%     group = [group; repmat('res/disk', size(t))];
    
    t = checkACaseOfExetimeOA(0, 1, fig);
    times = [times; t];
    group = [group; repmat('restricted', size(t))];
    
    boxplot(times, group);
    ylabel('times');
    
    figure(fig);
    legend('implicit', 'restricted', 'Location', 'NorthWest');
end

function times = checkACaseOfExetimeOA(implicit, physical, fig)
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

    file = '../log/revision/oppor_aggr/';
    if (implicit)
        file = [file, 'implicit/'];
    else
        file = [file, 'restricted/'];
    end
    if (physical)
        file = [file, 'physical/'];
    else
        file = [file, 'random/'];
    end
    file = [file, 'convert.txt'];

    x = importdata(file);
    data = x.data;
    %find only feasibles ones;
    data = data(data(:, 7)==1 & data(:, 8)==1, :);
    times = data(:, 11);
    fprintf('%s: max=%d, min=%d, mean=%.3f\n', str, max(times), min(times), mean(times));
    flows = data(:, 3);
    utils = data(:, 5);
    hyper = data(:, 14);
    channels = data(:, 6);
    
    figure;
    hold on;
    title([str, ' flows vs time']);
    for i=0:4
        ch = 2^i;
        y = [];
        for f = 1:50
            x = times(flows == f & channels == ch);
            if (length(x) ~= 0)
                y = [y, mean(x)];
            else
                y = [y, 0];
            end
        end
        plot(1:50, y);
    end

    figure;
    hold on;
    title([str, ' utils vs time']);
    for i=0:4
        ch = 2^i;
        y = [];
        for u=1:25
            x = times(utils > u-1 & utils <= u & channels == ch);
            if (length(x) ~= 0)
                y = [y, mean(x)];
            else
                y = [y, 0];
            end
        end
        plot(0.5:1:25, y);
    end
    
    xs = [];
    y = [];
    ystd = [];
    
    
    totTx = hyper .* utils / 10^4;
    maxt = ceil(max(totTx));
    step = 1;
    for t=step:step:maxt
        x = times(totTx > t-step & totTx <= t);
        if (length(x) ~= 0)
            xs = [xs, t-step/2];
            y = [y, mean(x)];
            ystd = [ystd, std(y)];
        end
    end
    
    figure(fig);
    if (implicit)
        if (physical)
            mark = '-^';
        else
            mark = '-o';
        end
    else
        if (physical)
            mark = '-*';
        else
            mark = '-s';
        end
    end
    errorbar(xs*10^4, y, ystd, mark);
    
    y = [];
    for i=0:4
        ch = 2^i;
        x = times(channels == ch);
        if (length(x) ~= 0)
            y = [y, mean(x)];
        else
            y = [y, 0];
        end
    end
    figure;
    plot(2.^[0:4], y);
    title([str, ' channels vs time']);
    
    mat = [];
    for ch=0:4
        arow = [];
        for f = 1:50
            x = times(channels == 2^ch & flows == f);
            if (length(x) ~= 0)
                arow = [arow, max(x)];
            else
                arow = [arow, 0];
            end 
        end
        mat = [mat; arow];
    end
    size(mat)
    [X,Y] = meshgrid(0:4, 1:50);
    figure;
    title(str);
    %mesh(X, Y, mat);
    bar3(mat);
end