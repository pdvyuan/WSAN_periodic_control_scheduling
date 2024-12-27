function maxBufCheck
    group = [];
    maxbuf = [];
    
    
%     m = checkACaseOfMaxBuf(1, 0);
%     maxbuf = [maxbuf; m];
%     group = [group; repmat('imp/disk', size(m, 1), size(m, 2))];
    
    m = checkACaseOfMaxBuf(1, 1);
    maxbuf = [maxbuf; m];
    group = [group; repmat('implicit  ', size(m, 1), size(m, 2))];
    
%     m = checkACaseOfMaxBuf(0, 0);
%     maxbuf = [maxbuf; m];
%     group = [group; repmat('res/disk', size(m, 1), size(m, 2))];
    
    m = checkACaseOfMaxBuf(0, 1);
    maxbuf = [maxbuf; m];
    group = [group; repmat('restricted', size(m, 1), size(m, 2))];
    
    boxplot(maxbuf, group);
    ylabel('maximum queue length');
    set(gca, 'FontSize', 15);
end

function maxmem = checkACaseOfMaxBuf(implicit, physical)
    % take LLF as example
    algoId = 9;
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

    file = '../log/revision/algorithms/';
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
    data = data(data(:, 5*algoId+2)==1, :);
    maxmem = data(:, 5*algoId+4);
    fprintf('%s: max=%d, min=%d, mean=%.3f\n', str, max(maxmem), min(maxmem), mean(maxmem));
    flows = data(:, 3);
    utils = data(:, 5);
    channels = data(:, 6);
    
    figure;
    hold on;
    title([str, ' flows vs mem']);
    for i=0:4
        ch = 2^i;
        y = [];
        for f = 1:50
            x = maxmem(flows == f & channels == ch);
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
    title([str, ' utils vs mem']);
    for i=0:4
        ch = 2^i;
        y = [];
        for u=1:16
            x = maxmem(utils > u-1 & utils <= u & channels == ch);
            if (length(x) ~= 0)
                y = [y, mean(x)];
            else
                y = [y, 0];
            end
        end
        plot(0.5:1:16, y);
    end
    
    y = [];
    for i=0:4
        ch = 2^i;
        x = maxmem(channels == ch);
        if (length(x) ~= 0)
            y = [y, mean(x)];
        else
            y = [y, 0];
        end
    end
    figure;
    plot(2.^[0:4], y);
    title([str, ' channels vs mem']);
    
    mat = [];
    for ch=0:4
        arow = [];
        for f = 1:50
            x = maxmem(channels == 2^ch & flows == f);
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